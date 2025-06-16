# gRPC API Documentation

This document outlines the gRPC services provided.

## Protobuf Definitions

The gRPC service and message definitions are organized by domain into the following files within the `proto/` directory:

-   `common.proto`: Defines common or example services (like `ExampleGreeter`).
-   `maps.proto`: Defines services and messages related to mapping functionalities (Geocoding, Places, Directions, Distance Matrix).
-   `gemini.proto`: Defines services and messages for interacting with the Gemini AI model.

Clients should use these definitions to generate stubs for communication. Each `.proto` file specifies its Go package path (e.g., `api-service/grpc/pb/maps` for `maps.proto`) which will be relevant for Go clients.

## Services

This section details the gRPC services provided by the API.

### Common Services (`common.proto`)

#### `ExampleGreeter` Service

This is a simple service for demonstration purposes.

**Service Name:** `common_proto.ExampleGreeter`

#### Methods

##### SayHello

Sends a greeting.

*   **Request:** `common_proto.HelloRequest`
    ```protobuf
    message HelloRequest {
      string name = 1;
    }
    ```
*   **Response:** `common_proto.HelloReply`
    ```protobuf
    message HelloReply {
      string message = 1;
    }
    ```

*   **Description:**
    Takes a `HelloRequest` with a `name` field and returns a `HelloReply` with a `message` field containing a greeting.

### Maps Services (`maps.proto`)

#### `Maps` Service
**Service Name:** `maps_proto.Maps`

Provides access to various Google Maps functionalities. Refer to `proto/maps.proto` for detailed message definitions.

##### Methods

###### `Geocode`
Converts a human-readable address into geographic coordinates.
*   **Request:** `maps_proto.GeocodeRequest` (contains `address`)
*   **Response:** `maps_proto.GeocodeResponse` (contains `results` with `formatted_address`, `location`, and `status`)

###### `SearchPlaces`
Searches for places based on a query string or location.
*   **Request:** `maps_proto.SearchPlacesRequest` (contains `query`, `location`)
*   **Response:** `maps_proto.SearchPlacesResponse` (contains `results` of type `maps_proto.Place` and `status`)

###### `GetPlaceDetails`
Retrieves detailed information about a specific place using its Place ID.
*   **Request:** `maps_proto.PlaceDetailsRequest` (contains `place_id`)
*   **Response:** `maps_proto.PlaceDetailsResponse` (contains `result` of type `maps_proto.Place` and `status`)

###### `GetDirections`
Provides directions between an origin and a destination.
*   **Request:** `maps_proto.DirectionsRequest` (contains `origin`, `destination`)
*   **Response:** `maps_proto.DirectionsResponse` (contains `routes`, `geocoded_waypoints`, and `status`)

###### `GetDistanceMatrix`
Calculates travel time and distance between one or more origins and destinations.
*   **Request:** `maps_proto.DistanceMatrixRequest` (contains `origins`, `destinations`)
*   **Response:** `maps_proto.DistanceMatrixResponse` (contains `origin_addresses`, `destination_addresses`, `rows` with element details, and `status`)

### AI Services (`gemini.proto`)

#### `Gemini` Service
**Service Name:** `gemini_proto.Gemini`

Provides access to Google's Gemini AI model for content generation. Refer to `proto/gemini.proto` for detailed message definitions.

##### Methods

###### `GenerateContent`
Sends a textual prompt to the Gemini model and receives generated content.
*   **Request:** `gemini_proto.GeminiRequest` (contains `prompt`)
*   **Response:** `gemini_proto.GeminiResponse` (contains `candidates` with generated `content`, `safety_ratings`, etc.)

## Connecting to the Server

The gRPC server in `api-service` runs on port `50051` by default (non-TLS).

Clients need to generate stubs from the relevant `.proto` files located in the `proto/` directory:
-   `common.proto` (for `ExampleGreeter` service)
-   `maps.proto` (for `Maps` service)
-   `gemini.proto` (for `Gemini` service)

For Go clients, the generated packages will correspond to the `go_package` options specified in these files:
-   `api-service/grpc/pb/common`
-   `api-service/grpc/pb/maps`
-   `api-service/grpc/pb/gemini`
