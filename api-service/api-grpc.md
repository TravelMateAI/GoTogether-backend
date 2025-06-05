# gRPC API Documentation

This document outlines the gRPC services provided.

## Proto Definition File

The service definitions can be found in `proto/communication.proto`.

## Services

### Greeter Service

This is a simple service for demonstration purposes.

**Service Name:** `proto.Greeter`

#### Methods

##### SayHello

Sends a greeting.

*   **Request:** `proto.HelloRequest`
    ```protobuf
    message HelloRequest {
      string name = 1;
    }
    ```
*   **Response:** `proto.HelloReply`
    ```protobuf
    message HelloReply {
      string message = 1;
    }
    ```

*   **Description:**
    Takes a `HelloRequest` with a `name` field and returns a `HelloReply` with a `message` field containing a greeting.

## Connecting to the Server

The gRPC server in `api-service` runs on port `50051` by default.
Clients should use the definitions from `proto/communication.proto` to generate stubs for communication.
