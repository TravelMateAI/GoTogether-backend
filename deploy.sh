#!/bin/bash

set -e

# Define Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

spinner() {
    local pid=$1
    local message="${2:-Building...}" # Default message if not provided
    local delay=0.1
    local spinstr='|/-\'
    echo -n -e "${YELLOW}${message} ${NC}"
    while ps -p "$pid" > /dev/null; do
        local temp_spinstr=${spinstr#?}
        printf "[%c]" "$spinstr"
        spinstr="$temp_spinstr${spinstr%"$temp_spinstr"}" # Rotate spinner
        sleep $delay
        printf "\b\b\b" # Erase the spinner
    done
    wait "$pid" # Wait for the process to finish and get its exit code
    local exit_code=$?
    if [ $exit_code -eq 0 ]; then
        echo -e "\r${GREEN}${message} [OK]${NC}      " # Success
    else
        echo -e "\r${RED}${message} [FAIL]${NC}    " # Fail
    fi
    return $exit_code # Return the exit code of the command
}

log_message() {
    local message="$1"
    local color="${2:-$NC}" # Default to No Color if not specified
    echo -e "${color}[$(date '+%Y-%m-%d %H:%M:%S')] ${message}${NC}"
}

SECTION_SEPARATOR="========================================================================"

COMPOSE_FILE="deploy/docker-compose.yml"

SERVICES=(
  "auth-service"
  "api-gateway"
  "api-service"
  "planning-service"
   "social-media-service"
)


usage() {
  echo -e "${BLUE}Usage: $0 [--build] [--run] [--up] [--down] [--logs] [--status] [--force-rebuild] [service1 service2 ...]${NC}"
  echo "  --help, -h      Show this help message and exit"
  echo "  --up            Build and then run services (equivalent to --build --run)"
  echo "  --build         Build only"
  echo "  --run           Run only"
  echo "  --down          Stop and remove all services defined in the compose file"
  echo "  --logs [service...] View logs for specified services (or all if none specified)"
  echo "  --status        Show the status of services"
  echo "  --force-rebuild Force rebuild of images without using cache"
  echo "  [service ...]   Optional list of services to target"
  echo
  echo "Examples:"
  echo "  $0                      # Build and run all"
  echo "  $0 --build              # Build all"
  echo "  $0 --run                # Run all"
  echo "  $0 --build api-gateway"
  echo "  $0 --run api-gateway department-service"
  exit 1
}

BUILD=false
RUN=false
DOWN_CMD=false
LOGS_CMD=false
STATUS_CMD=false
FORCE_REBUILD=false

# Parse flags
while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h)
      usage
      exit 0
      ;;
    --up)
      BUILD=true
      RUN=true
      shift
      ;;
    --build)
      BUILD=true
      shift
      ;;
    --run)
      RUN=true
      shift
      ;;
    --down)
      DOWN_CMD=true
      shift
      ;;
    --logs)
      LOGS_CMD=true
      shift
      ;;
    --status)
      STATUS_CMD=true
      shift
      ;;
    --force-rebuild)
      FORCE_REBUILD=true
      shift
      ;;
    -*)
      usage
      ;;
    *)
      break
      ;;
  esac
done

# If no operational flags specified and no services listed, default to build and run all.
if [ "$BUILD" = false ] && [ "$RUN" = false ] && [ "$DOWN_CMD" = false ] && [ "$LOGS_CMD" = false ] && [ "$STATUS_CMD" = false ] && [ $# -eq 0 ]; then
  BUILD=true
  RUN=true
  TARGET_SERVICES=("${SERVICES[@]}") # Ensure TARGET_SERVICES is set for default all
elif [ $# -gt 0 ]; then # Services are listed (these are remaining arguments after flags)
    TARGET_SERVICES=("$@")
else # Some flags were specified, but no services - applies to all services
    TARGET_SERVICES=("${SERVICES[@]}")
fi

# If no TARGET_SERVICES determined (e.g. only flags like --build passed, no specific services), set to all.
if [ ${#TARGET_SERVICES[@]} -eq 0 ]; then
  TARGET_SERVICES=("${SERVICES[@]}")
fi

# Down command block
if $DOWN_CMD; then
  log_message "Starting Shutdown Process" "$BLUE"
  echo "$SECTION_SEPARATOR"
  log_message "🔽 Stopping and removing all services defined in $COMPOSE_FILE..." "$BLUE"
  docker-compose -f "$COMPOSE_FILE" down
  log_message "✅ All services stopped and removed." "$GREEN"
  echo "$SECTION_SEPARATOR"
  log_message "Shutdown Process Completed" "$GREEN"
  # If only --down was specified (and no other action flags), exit.
  if ! $BUILD && ! $RUN ; then
    exit 0
  fi
fi

# Validate service names
for svc in "${TARGET_SERVICES[@]}"; do
  if [[ ! " ${SERVICES[@]} " =~ " ${svc} " ]]; then
    echo -e "${RED}❌ Unknown service: $svc${NC}"
    echo -e "${GREEN}✅ Known services: ${SERVICES[*]}${NC}"
    exit 1
  fi
done

# Build block
if $BUILD; then
  log_message "Starting Build Process" "$BLUE"
  echo "$SECTION_SEPARATOR"
  # echo -e "${BLUE}🔨 Building: ${TARGET_SERVICES[*]}${NC}" # Original overall message, now handled by log_message
  for svc in "${TARGET_SERVICES[@]}"; do
    if [ -f "$svc/pom.xml" ]; then
      log_message "📦 Running Maven build for $svc..." "$YELLOW"
      (cd "$svc" && mvn clean package -DskipTests)
    elif [ -f "$svc/build.gradle" ]; then
      log_message "📦 Running Gradle build for $svc..." "$YELLOW"
      (cd "$svc" && ./gradlew bootJar --no-daemon)
    else
      log_message "ℹ️ Skipping Maven/Gradle build for $svc (no pom.xml or build.gradle)" "$YELLOW"
    fi

    build_cmd_array=() # Declare as a regular array
    build_cmd_array+=("docker-compose" "-f" "$COMPOSE_FILE" "build")
    if $FORCE_REBUILD; then
      build_cmd_array+=("--no-cache")
      log_message "ℹ️ Force rebuild enabled for $svc (using --no-cache)." "$YELLOW"
    fi
    build_cmd_array+=("$svc")

    # Run the build command
    ("${build_cmd_array[@]}") &
    spinner $! "🐳 Building Docker image for $svc"
    if [ $? -ne 0 ]; then
        # Spinner already prints FAIL, this adds more context and exits
        log_message "❌ Docker build failed for $svc. Aborting." "$RED"
        exit 1 # Exit if build fails, respecting set -e implicitly
    fi
  done
  echo "$SECTION_SEPARATOR"
  log_message "Build Process Completed" "$GREEN"
fi

# Run block
if $RUN; then
  log_message "Starting Services" "$BLUE"
  echo "$SECTION_SEPARATOR"
  log_message "🚀 Starting: ${TARGET_SERVICES[*]}" "$BLUE"
  docker-compose -f "$COMPOSE_FILE" up -d "${TARGET_SERVICES[@]}"
  echo "$SECTION_SEPARATOR"
  log_message "Services Startup Initiated" "$GREEN"

  log_message "Exposed Port Mappings:" "$BLUE"
  echo "$SECTION_SEPARATOR"

  service_ports_info=""
  # current_service="" # Removed as unused
  in_service_block=false
  in_ports_block=false

  for svc_name in "${TARGET_SERVICES[@]}"; do
      service_ports_info=""
      service_ports_info=$(awk -v service="$svc_name" '
          BEGIN {
              in_service = 0;
              in_ports = 0;
              service_header_printed = 0;
          }
          $1 == service":" {
              in_service = 1;
              next;
          }
          in_service && /^[[:space:]]+ports:/ {
              in_ports = 1;
              next;
          }
          in_service && in_ports && /^[[:space:]]+-/ {
              if (!service_header_printed) {
                  print "  " service ":";
                  service_header_printed = 1;
              }
              sub(/^[[:space:]]+- +/, "    - ");
              print;
              next;
          }
          in_service && in_ports && (!/^[[:space:]]+/ || $1 ~ /^[a-zA-Z0-9_-]+:/) {
              in_ports = 0;
              in_service = 0;
          }
          in_service && !in_ports && $1 ~ /^[a-zA-Z0-9_-]+:/ && $1 != service":" {
              in_service = 0;
          }
      ' "$COMPOSE_FILE")

      if [ -n "$service_ports_info" ]; then
          log_message "$service_ports_info" "$GREEN"
      else
          log_message "  $svc_name: No port mappings found or error in parsing." "$YELLOW"
      fi
  done
  echo "$SECTION_SEPARATOR"
fi

# Logs command block
if $LOGS_CMD; then
  log_message "Fetching Logs" "$BLUE"
  echo "$SECTION_SEPARATOR"
  # TARGET_SERVICES should be already set by prior logic (either to specific services or all)
  log_message "📄 Fetching logs for: ${TARGET_SERVICES[*]}..." "$BLUE"
  docker-compose -f "$COMPOSE_FILE" logs -f "${TARGET_SERVICES[@]}"
  # If only --logs was specified (and no other action flags like build, run, down), then exit.
  if ! $BUILD && ! $RUN && ! $DOWN_CMD; then
    exit 0
  fi
fi

# Status command block
if $STATUS_CMD; then
  log_message "Checking Service Status" "$BLUE"
  echo "$SECTION_SEPARATOR"
  log_message "ℹ️ Service Status for: ${TARGET_SERVICES[*]}..." "$BLUE"
  docker-compose -f "$COMPOSE_FILE" ps "${TARGET_SERVICES[@]}"
  echo "$SECTION_SEPARATOR" # Add this
  log_message "Service Status Check Completed" "$GREEN" # Add this
  # If only --status was specified, exit.
  if ! $BUILD && ! $RUN && ! $DOWN_CMD && ! $LOGS_CMD; then
    exit 0
  fi
fi