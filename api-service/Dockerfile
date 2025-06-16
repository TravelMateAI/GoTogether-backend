# Builder stage
FROM golang:1.24.3-alpine AS builder

WORKDIR /app

# Copy go.mod and go.sum and download dependencies
COPY go.mod go.sum ./
RUN go mod download

# Copy the rest of the source code
COPY . .

# Build the application
RUN CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -ldflags="-w -s" -o /api-service .

# Final stage
FROM alpine:latest

# Copy the built binary from the builder stage
COPY --from=builder /api-service /api-service

# Expose ports
EXPOSE 8000
EXPOSE 50051

# Set the entrypoint
ENTRYPOINT ["/api-service"]
