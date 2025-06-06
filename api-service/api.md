# API Service Endpoints Documentation

## 1. Geocode Address

Converts a human-readable address into geographic coordinates (latitude and longitude).

- **Path:** `/maps/geocode`
- **Method:** `GET`
- **Controller:** `controllers.GeocodeController`
- **Service:** `services.maps.GeocodeService.Geocode()`

### Request

#### Query Parameters:

- `address` (string, required): The street address that you want to geocode.
  - Example: `1600 Amphitheatre Parkway, Mountain View, CA`

#### Example `curl` Request:

```bash
curl "http://<your_api_service_host>:<port>/maps/geocode?address=1600%20Amphitheatre%20Parkway,%20Mountain%20View,%20CA"
```

### Response

The response is a JSON object mirroring the Google Maps Geocoding API response.

#### Success (200 OK)

- **Content-Type:** `application/json`

##### Body Structure:

```json
{
  "results": [
    {
      "formatted_address": "1600 Amphitheatre Pkwy, Mountain View, CA 94043, USA",
      "geometry": {
        "location": {
          "lat": 37.4224764,
          "lng": -122.0842499
        }
        // ... other geometry details from Google API might be present
      }
      // ... other address components and details from Google API might be present
    }
    // ... potentially more results if the address is ambiguous
  ],
  "status": "OK" // Or other Google API statuses like "ZERO_RESULTS"
}
```

#### Error Responses:

- **500 Internal Server Error:** If the backend service fails to connect to the Google API or encounters other issues.
  ```json
  {
    "error": "failed to get geocode: <Google API error message or internal error>"
  }
  ```
- Other error statuses from Google Geocoding API (e.g., `REQUEST_DENIED`, `INVALID_REQUEST`) will be reflected in the `status` field of a 200 OK response from our service, but the body will contain Google's error message. The service's `Geocode` function returns an error if `resp.StatusCode != http.StatusOK`, which the controller then turns into a 500.

---
## 2. Search Places

Searches for places using either a text query or geographic coordinates (for nearby searches). This endpoint now includes fully constructed photo URLs in the response.

- **Path:** `/maps/places`
- **Method:** `GET`
- **Controller:** `controllers.LocationDataController`
- **Service:** `services.maps.PlacesService.SearchPlaces()`

### Request

#### Query Parameters:

- `query` (string, optional): The text string to search for (e.g., "restaurants in Sydney", "Eiffel Tower"). If provided, a text search is performed.
- `location` (string, optional): Latitude and longitude coordinates (e.g., "37.4224764,-122.0842499"). If `query` is not provided or is empty, and `location` is provided, a nearby search is performed.
  - *Note:* The service's `SearchPlaces` function prioritizes `query` over `location` if both are provided.

#### Example `curl` Requests:

1.  **Text Search:**
    ```bash
    curl "http://<your_api_service_host>:<port>/maps/places?query=restaurants%20in%20Sydney"
    ```

2.  **Nearby Search (when `query` is omitted):**
    ```bash
    curl "http://<your_api_service_host>:<port>/maps/places?location=37.4224764,-122.0842499"
    ```

### Response

The response is a JSON object based on the Google Maps Places API response, with the addition of the `photo_urls` field.

#### Success (200 OK)

- **Content-Type:** `application/json`

##### Body Structure (`models.PlacesResponse`):

```json
{
  "results": [
    {
      "name": "Example Restaurant",
      "vicinity": "123 Main St, Anytown", // Or "address" depending on Google's field name, mapped from "vicinity"
      "rating": 4.5,
      "user_ratings_total": 100,
      "place_id": "ChIJN1t_tDeuEmsRUsoyG83frY4",
      "types": ["restaurant", "food", "point_of_interest"],
      "geometry": {
        "location": {
          "lat": -33.8670522,
          "lng": 151.1957362
        }
      },
      "opening_hours": { // Present if available from Google
        "open_now": true
      },
      "photos": [ // Original 'photos' array from Google API, may contain photo_reference
        {
          "photo_reference": "CmRaAAAA...",
          "height": 800,
          "width": 1200
          // ... other HTML attributions if present
        }
      ],
      "photo_urls": [ // <<< NEW FIELD >>> Array of fully constructed photo URLs
        "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=CmRaAAAA...&key=YOUR_GOOGLE_MAPS_API_KEY"
        // ... more URLs if multiple photos exist
      ]
    }
    // ... other results
  ],
  "status": "OK" // Or other Google API statuses like "ZERO_RESULTS"
}
```

#### Error Responses:

- **500 Internal Server Error:** If the backend service fails to connect to the Google API, encounters issues during processing, or if neither `query` nor `location` is provided.
  ```json
  {
    "error": "failed to search places: <Google API error message or internal error>"
    // or "either query or location must be provided"
  }
  ```
- Google API errors (e.g., `REQUEST_DENIED`, `INVALID_REQUEST`) will be reflected in the `status` field of a 200 OK response (if the error occurs at Google's end but after a successful HTTP call), or could lead to a 500 error if our service treats it as a failed fetch.

---
## 3. Get Directions

Provides directions between two locations (origin and destination).

- **Path:** `/maps/directions`
- **Method:** `GET`
- **Controller:** `controllers.DirectionsController`
- **Service:** `services.maps.DirectionsService.GetDirections()`

### Request

#### Query Parameters:

- `origin` (string, required): The starting point for the directions.
  - Example: `Toronto` or `43.6532,-79.3832`
- `destination` (string, required): The ending point for the directions.
  - Example: `Montreal` or `45.5017,-73.5673`

#### Example `curl` Request:

```bash
curl "http://<your_api_service_host>:<port>/maps/directions?origin=Toronto&destination=Montreal"
```

### Response

The response is a JSON object mirroring the Google Maps Directions API response. The structure can be complex, providing routes, legs, steps, and polylines.

#### Success (200 OK)

- **Content-Type:** `application/json`

##### Body Structure (Simplified Example):

```json
{
  "routes": [
    {
      "summary": "ON-401 E",
      "legs": [
        {
          "distance": {
            "text": "542 km",
            "value": 542200 // meters
          },
          "duration": {
            "text": "5 hours 30 mins",
            "value": 19800 // seconds
          },
          "start_address": "Toronto, ON, Canada",
          "end_address": "Montreal, QC, Canada",
          "steps": [ // Array of step-by-step instructions
            {
              "html_instructions": "Head east on Queen St W toward Bay St",
              "distance": {"text": "0.6 km", "value": 600},
              "duration": {"text": "1 min", "value": 60},
              "polyline": {
                "points": "encoded_polyline_string"
              }
              // ... more step details
            }
            // ... more steps
          ]
        }
        // ... more legs if waypoints were involved (not supported by this simple endpoint)
      ],
      "overview_polyline": {
        "points": "encoded_overview_polyline_string"
      },
      "warnings": [],
      "bounds": { // Northeast and southwest coordinates of the bounding box for the route
        "northeast": {"lat": 45.5017, "lng": -73.5673},
        "southwest": {"lat": 43.6532, "lng": -79.3832}
      }
      // ... other route details
    }
    // ... potentially more routes
  ],
  "geocoded_waypoints": [ // Status for origin and destination geocoding
    {"geocoder_status": "OK", "place_id": "ChIJpTvG15DL1IkRd8S0Kl9NkvU", "types": ["locality", "political"]},
    {"geocoder_status": "OK", "place_id": "ChIJDbdkHFQayUwR7-8fMbi8JUA", "types": ["locality", "political"]}
  ],
  "status": "OK" // Or other Google API statuses like "ZERO_RESULTS", "NOT_FOUND"
}
```

#### Error Responses:

- **500 Internal Server Error:** If the backend service fails to connect to the Google API or encounters other issues.
  ```json
  {
    "error": "failed to get directions: <Google API error message or internal error>"
  }
  ```
- Google API errors (e.g., `NOT_FOUND`, `ZERO_RESULTS`, `REQUEST_DENIED`) will be reflected in the `status` field of the main JSON response (if the HTTP call itself was okay).

---
## 4. Get Distance Matrix

Calculates travel time and distance between one or more origins and destinations.

- **Path:** `/maps/distance-matrix`
- **Method:** `GET`
- **Controller:** `controllers.DistanceMatrixController`
- **Service:** `services.maps.DistanceMatrixService.GetDistanceMatrix()`

### Request

#### Query Parameters:

- `origins` (string, required): One or more addresses, place IDs, or latitude/longitude coordinates from which to calculate distance and time. If you pass multiple locations, separate them with the pipe character (`|`).
  - Example: `Vancouver BC|Seattle` or `49.2827,-123.1207|47.6062,-122.3321`
- `destinations` (string, required): One or more addresses, place IDs, or latitude/longitude coordinates to which to calculate distance and time. If you pass multiple locations, separate them with the pipe character (`|`).
  - Example: `San Francisco|Victoria BC` or `37.7749,-122.4194|48.4284,-123.3656`

#### Example `curl` Request:

```bash
curl "http://<your_api_service_host>:<port>/maps/distance-matrix?origins=Vancouver%20BC|Seattle&destinations=San%20Francisco|Victoria%20BC"
```

### Response

The response is a JSON object containing origin and destination addresses as returned by Google, and rows corresponding to each origin, each containing elements corresponding to each destination.

#### Success (200 OK)

- **Content-Type:** `application/json`

##### Body Structure (`maps.DistanceMatrixResponse`):

```json
{
  // Note: The actual Google API response includes "origin_addresses" and "destination_addresses" arrays here.
  // The current Go service model DistanceMatrixResponse does not explicitly map these,
  // but they would be part of the raw response from Google if you inspected it before decoding.
  // The Go model focuses on the "rows" part.
  "rows": [
    { // Corresponds to the first origin
      "elements": [
        { // Corresponds to the first origin vs first destination
          "status": "OK", // e.g., "OK", "NOT_FOUND", "ZERO_RESULTS"
          "duration": {
            "text": "14 hours 4 mins", // Human-readable duration
            "value": 50640        // Duration in seconds
          },
          "distance": {
            "text": "1,540 km",    // Human-readable distance
            "value": 1540123      // Distance in meters
          }
        },
        { // Corresponds to the first origin vs second destination
          "status": "OK",
          "duration": {
            "text": "3 hours 0 mins",
            "value": 10800
          },
          "distance": {
            "text": "110 km",
            "value": 109987
          }
        }
        // ... more elements if more destinations
      ]
    },
    { // Corresponds to the second origin
      "elements": [
        { // Corresponds to the second origin vs first destination
          "status": "OK",
          "duration": {
            "text": "12 hours 30 mins",
            "value": 45000
          },
          "distance": {
            "text": "1,350 km",
            "value": 1350000
          }
        },
        { // Corresponds to the second origin vs second destination
          "status": "OK",
          "duration": {
            "text": "1 hour 30 mins",
            "value": 5400
          },
          "distance": {
            "text": "70 km",
            "value": 70000
          }
        }
        // ... more elements
      ]
    }
    // ... more rows if more origins
  ]
  // The Google API also includes a general "status" field for the overall request here.
  // Example: "status": "OK"
  // This is not explicitly in the current Go service model DistanceMatrixResponse.
}
```
*Note: The Go model `DistanceMatrixResponse` primarily focuses on the `rows`. The full Google API response also includes `origin_addresses`, `destination_addresses`, and an overall `status` field for the request, which are not explicitly defined in the current Go model but are part of the raw JSON from Google.*

#### Error Responses:

- **500 Internal Server Error:** If the backend service fails to connect to the Google API or encounters other issues (e.g., JSON decoding failure).
  ```json
  {
    "error": "failed to get distance matrix: <Google API error message or internal error>"
  }
  ```
- Element-level errors (e.g., a specific origin/destination pair not found) are indicated by the `status` field within each element (e.g., `"NOT_FOUND"`, `"ZERO_RESULTS"`). The overall HTTP status will still be 200 OK if the API call itself succeeded.

---
## 5. Get AI Response (Gemini)

Sends a prompt to a Google Gemini model ("gemini-2.0-flash") and returns its response.

- **Path:** `/gemini/`
- **Method:** `GET`
- **Controller:** `controllers.GeminiController`
- **Service:** `services.gemini.AIService.GetAIResponse()`
- **Authentication:** Requires `GOOGLE_GEMINI_API_KEY` to be set in the service's environment.

### Request

#### Query Parameters:

- `prompt` (string, required): The prompt or question to send to the AI model.
  - Example: `Explain quantum computing in simple terms.`

#### Example `curl` Request:

```bash
curl "http://<your_api_service_host>:<port>/gemini/?prompt=Explain%20quantum%20computing%20in%20simple%20terms."
```

### Response

The response is a JSON object based on the Google Gemini API's `GenerateContentResponse`. The exact structure can vary based on the model and request options.

#### Success (200 OK)

- **Content-Type:** `application/json`

##### Body Structure (Simplified Example of `genai.GenerateContentResponse`):

```json
{
  "candidates": [
    {
      "content": {
        "parts": [
          {
            "text": "Imagine a regular computer uses bits, which are like light switches that are either ON or OFF (0 or 1). Quantum computers use 'qubits'. A qubit is like a dimmer switch that can be ON, OFF, or somewhere in between. It can even be both ON and OFF at the same time (superposition)! This allows quantum computers to explore many possibilities at once, making them super fast for certain complex problems that regular computers would take billions of years to solve, like discovering new medicines or materials."
          }
        ],
        "role": "model"
      },
      "finish_reason": "STOP", // e.g., STOP, MAX_TOKENS, SAFETY, RECITATION, OTHER
      "index": 0,
      "safety_ratings": [ // Array of safety ratings for different categories
        {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "probability": "NEGLIGIBLE"},
        {"category": "HARM_CATEGORY_HATE_SPEECH", "probability": "NEGLIGIBLE"},
        {"category": "HARM_CATEGORY_HARASSMENT", "probability": "NEGLIGIBLE"},
        {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "probability": "NEGLIGIBLE"}
      ]
      // "citation_metadata" might be present if content is sourced
    }
  ],
  "prompt_feedback": { // Feedback on the prompt itself, if applicable
    "safety_ratings": [
        {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "probability": "NEGLIGIBLE"},
        {"category": "HARM_CATEGORY_HATE_SPEECH", "probability": "NEGLIGIBLE"},
        {"category": "HARM_CATEGORY_HARASSMENT", "probability": "NEGLIGIBLE"},
        {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "probability": "NEGLIGIBLE"}
    ]
    // "block_reason" might be present if prompt was blocked
  }
}
```
*Note: The main generated text is typically found within `candidates[0].content.parts[0].text`.*

#### Error Responses:

- **500 Internal Server Error:** If the backend service fails to initialize the AI client, connect to the Gemini API, or encounters other issues.
  ```json
  {
    "error": "<Error message from AI service, e.g., error initializing client, error generating content>"
  }
  ```
- Errors from the Gemini API itself (e.g., authentication issues if API key is wrong, rate limits) would also typically result in a 500 error from this endpoint with a message from the underlying client library.

---
## 6. Get Place Details by ID

Retrieves detailed information for a specific place given its Google Place ID. This includes name, address, coordinates, photos (with fully constructed URLs), reviews, opening hours, and more.

- **Path:** `/maps/place/:place_id`
- **Method:** `GET`
- **Controller:** `controllers.PlaceDetailsController`
- **Service:** `services.maps.PlacesService.FindPlaceByID()`

### Request

#### Path Parameters:

- `place_id` (string, required): The Google Place ID for the location you want details for.
  - Example: `ChIJN1t_tDeuEmsRUsoyG83frY4` (This is an example ID for Google Sydney)

#### Example `curl` Request:

```bash
curl "http://<your_api_service_host>:<port>/maps/place/ChIJN1t_tDeuEmsRUsoyG83frY4"
```

### Response

The response is a JSON object containing the detailed place information.

#### Success (200 OK)

- **Content-Type:** `application/json`

##### Body Structure (`models.PlaceDetailResponse`):

```json
{
  "result": {
    "name": "Google Building 43",
    "formatted_address": "1600 Amphitheatre Pkwy, Mountain View, CA 94043, USA",
    "place_id": "ChIJN1t_tDeuEmsRUsoyG83frY4", // Example, will match requested ID
    "types": ["establishment", "point_of_interest"],
    "geometry": {
      "location": {
        "lat": 37.4224764,
        "lng": -122.0842499
      }
    },
    "rating": 4.5,
    "user_ratings_total": 150,
    "photos": [ // Original photo references from Google
      {
        "photo_reference": "CmRaAAAA...",
        "height": 1024,
        "width": 768,
        "html_attributions": ["&lt;a href=\"https://maps.google.com/maps/contrib/1000...\"&gt;A User&lt;/a&gt;"]
      }
    ],
    "photo_urls": [ // <<< Fully constructed photo URLs >>>
      "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=CmRaAAAA...&key=YOUR_GOOGLE_MAPS_API_KEY"
    ],
    "reviews": [
      {
        "author_name": "Jane Doe",
        "rating": 5,
        "relative_time_description": "a month ago",
        "text": "Great place!",
        "time": 1609459200
        // other review fields like author_url, profile_photo_url
      }
    ],
    "website": "https://www.google.com/",
    "international_phone_number": "+1 650-253-0000",
    "formatted_phone_number": "(650) 253-0000",
    "opening_hours": {
      "open_now": true,
      "periods": [
        {"open": {"day": 1, "time": "0800"}, "close": {"day": 1, "time": "1800"}}
        // ... other periods
      ],
      "weekday_text": [
        "Monday: 8:00 AM – 6:00 PM",
        // ... other days
      ],
      "permanently_closed": false
    },
    "vicinity": "1600 Amphitheatre Parkway, Mountain View",
    "utc_offset_minutes": -420,
    "address_components": [
        // ... array of address components
    ]
    // ... other fields like utc_offset_minutes, address_components as defined in model
  },
  "status": "OK",
  "html_attributions": [],
  "error_message": null // or empty string
}
```

#### Error Responses:

- **400 Bad Request:** If `place_id` is missing or malformed (based on Google's validation).
  - Response body example (if `placeDetails.Status == "INVALID_REQUEST"`):
    ```json
    {
      "result": {}, // Or minimal result structure
      "status": "INVALID_REQUEST",
      "error_message": "The place ID '...' is not valid." // Example message from Google
    }
    ```
- **403 Forbidden:** If the API key is invalid or lacks permissions for Places Details.
  - Response body example (if `placeDetails.Status == "REQUEST_DENIED"`):
    ```json
    {
      "result": {},
      "status": "REQUEST_DENIED",
      "error_message": "Your request was denied." // Example message from Google
    }
    ```
- **404 Not Found:** If the `place_id` does not correspond to a known place.
  - Response body example (if `placeDetails.Status == "NOT_FOUND"`):
    ```json
    {
      "result": {}, // Or minimal result structure
      "status": "NOT_FOUND",
      "error_message": null // Error message might be null if status is NOT_FOUND
    }
    ```
- **429 Too Many Requests:** If the API key has exceeded its query limit.
  - Response body example (if `placeDetails.Status == "OVER_QUERY_LIMIT"`):
    ```json
    {
      "result": {},
      "status": "OVER_QUERY_LIMIT",
      "error_message": "You have exceeded your daily request quota for this API." // Example
    }
    ```
- **500 Internal Server Error:** For other backend issues or unexpected errors from Google (e.g., `UNKNOWN_ERROR`).
  - Response body example:
    ```json
    {
      "error": "Failed to retrieve place details",
      "details": "<specific error message from service or Google>"
    }
    ```
    Or, if Google returns an `UNKNOWN_ERROR` status within the response object:
    ```json
    {
      "result": {},
      "status": "UNKNOWN_ERROR",
      "error_message": "An unexpected error occurred." // Example
    }
    ```

---
