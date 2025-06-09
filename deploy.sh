#!/bin/bash

set -e

COMPOSE_FILE="deploy/docker-compose.yml"

SERVICES=(
  "api-gateway"
  "planning-service"
  "social-media-service"
  "auth-service"
  "api-service"
)


usage() {
  echo "Usage: $0 [--build] [--run] [service1 service2 ...]"
  echo "  --build         Build only"
  echo "  --run           Run only"
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

# Parse flags
while [[ $# -gt 0 ]]; do
  case "$1" in
    --build)
      BUILD=true
      shift
      ;;
    --run)
      RUN=true
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

# If no flags or services specified: build and run all
if [ "$BUILD" = false ] && [ "$RUN" = false ] && [ $# -eq 0 ]; then
  BUILD=true
  RUN=true
  TARGET_SERVICES=("${SERVICES[@]}")
else
  TARGET_SERVICES=("$@")
fi

if [ ${#TARGET_SERVICES[@]} -eq 0 ]; then
  TARGET_SERVICES=("${SERVICES[@]}")
fi

# Validate service names
for svc in "${TARGET_SERVICES[@]}"; do
  if [[ ! " ${SERVICES[@]} " =~ " ${svc} " ]]; then
    echo "❌ Unknown service: $svc"
    echo "✅ Known services: ${SERVICES[*]}"
    exit 1
  fi
done

# Build block
if $BUILD; then
  echo "🔨 Building: ${TARGET_SERVICES[*]}"
  for svc in "${TARGET_SERVICES[@]}"; do
    if [ -f "$svc/pom.xml" ]; then
      echo "📦 Running Maven build for $svc..."
      (cd "$svc" && mvn clean package -DskipTests)
    elif [ -f "$svc/build.gradle" ]; then
      echo "📦 Running Gradle build for $svc..."
      (cd "$svc" && ./gradlew bootJar --no-daemon)
    else
      echo "ℹ️ Skipping Maven/Gradle build for $svc (no pom.xml or build.gradle)" 
    fi

    echo "🐳 Building Docker image for $svc..."
    docker-compose -f "$COMPOSE_FILE" build "$svc"
  done
fi

# Run block
if $RUN; then
  echo "🚀 Starting: ${TARGET_SERVICES[*]}"
  docker-compose -f "$COMPOSE_FILE" up -d "${TARGET_SERVICES[@]}"
fi