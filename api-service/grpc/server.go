package grpc

import (
	"context"
	"log"
	// "strconv" // For potential string to number conversions - Not used yet
	// "strings" // For potential string manipulations (e.g. lat,lng parsing) - Not used yet

	pb "api-service/grpc/pb" // Updated generated proto package
	"api-service/services/maps"
	// "api-service/services/gemini" // Existing gemini service - Not used yet
	// gmaps "googlemaps.github.io/maps" // No longer used by Geocode, remove if not used elsewhere

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// ExampleGreeterServerImpl is used to implement proto.ExampleGreeterServer.
type ExampleGreeterServerImpl struct {
	pb.UnimplementedExampleGreeterServer
}

// SayHello implements proto.ExampleGreeterServer
func (s *ExampleGreeterServerImpl) SayHello(ctx context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
	log.Printf("Received gRPC SayHello request: %v", in.GetName())
	return &pb.HelloReply{Message: "Hello " + in.GetName()}, nil
}

// MapsServerImpl implements pb.MapsServer
type MapsServerImpl struct {
	pb.UnimplementedMapsServer
	// You might inject actual service clients here if they are not singletons
	// For example:
	// geocodeService *maps.GeocodeService
	// placesService  *maps.PlacesService
	// etc.
	// For now, we'll assume direct calls to service package functions that handle their own client init.
}

// GeminiServerImpl implements pb.GeminiServer
type GeminiServerImpl struct {
	pb.UnimplementedGeminiServer
	// aiService *gemini.AIService // Example if injecting
}

// --- MapsService Implementation ---

func (s *MapsServerImpl) Geocode(ctx context.Context, req *pb.GeocodeRequest) (*pb.GeocodeResponse, error) {
	log.Printf("Received gRPC Geocode request for address: %s", req.GetAddress())

	// 1. Instantiate and call the existing service
	geoService := maps.NewGeocodeService() // Instantiating the service
	serviceResponse, err := geoService.Geocode(req.GetAddress())

	if err != nil {
		log.Printf("Geocode service error: %v", err)
		// Consider mapping specific errors to gRPC status codes if possible
		return nil, status.Errorf(codes.Internal, "Geocoding failed: %v", err)
	}

	// 2. Convert map[string]interface{} response to pb.GeocodeResponse
	var pbResults []*pb.GeocodeResponse_Result

	var responseStatus string
	if val, ok := serviceResponse["status"].(string); ok {
		responseStatus = val
	} else {
		// If status is not a string or not present, it's an unexpected response format
		log.Printf("Geocode service response missing or invalid 'status' field: %v", serviceResponse)
		return nil, status.Errorf(codes.Internal, "Geocoding response format error: missing status")
	}

	if responseStatus != "OK" && responseStatus != "ZERO_RESULTS" {
        log.Printf("Geocode service returned non-OK status: %s", responseStatus)
	}

	if resultsData, ok := serviceResponse["results"].([]interface{}); ok {
		for _, item := range resultsData {
			if resultMap, ok := item.(map[string]interface{}); ok {
				pbRes := &pb.GeocodeResponse_Result{}
				if formattedAddr, ok := resultMap["formatted_address"].(string); ok {
					pbRes.FormattedAddress = formattedAddr
				}

				if geometry, ok := resultMap["geometry"].(map[string]interface{}); ok {
					if location, ok := geometry["location"].(map[string]interface{}); ok {
						lat, latOk := location["lat"].(float64)
						lng, lngOk := location["lng"].(float64)
						if latOk && lngOk {
							pbRes.Location = &pb.GeocodeResponse_Location{Lat: lat, Lng: lng}
						}
					}
				}
				pbResults = append(pbResults, pbRes)
			}
		}
	} else if responseStatus == "OK" {
        log.Printf("Geocode service response 'OK' but 'results' field is missing or invalid: %v", serviceResponse)
        return nil, status.Errorf(codes.Internal, "Geocoding response format error: results field issue")
    }

	return &pb.GeocodeResponse{
		Results: pbResults,
		Status:  responseStatus,
	}, nil
}

func (s *MapsServerImpl) SearchPlaces(ctx context.Context, req *pb.SearchPlacesRequest) (*pb.SearchPlacesResponse, error) {
	log.Printf("Received gRPC SearchPlaces request: query='%s', location='%s'", req.GetQuery(), req.GetLocation())

	placesService := maps.NewPlacesService() // Instantiate the service
	serviceResponse, err := placesService.SearchPlaces(req.GetQuery(), req.GetLocation())

	if err != nil {
		log.Printf("SearchPlaces service error: %v", err)
		return nil, status.Errorf(codes.Internal, "SearchPlaces failed: %v", err)
	}

	if serviceResponse == nil {
		log.Printf("SearchPlaces service returned nil response")
		return nil, status.Errorf(codes.Internal, "SearchPlaces service returned nil response")
	}

	// Convert models.PlacesResponse to pb.SearchPlacesResponse
	var pbPlaces []*pb.Place
	for _, placeResultItem := range serviceResponse.Results {
		pbPlace := &pb.Place{
			PlaceId:          placeResultItem.PlaceID,
			Name:             placeResultItem.Name,
			Vicinity:         placeResultItem.Address, // 'Address' in model is 'vicinity' in JSON
			Rating:           placeResultItem.Rating,
			UserRatingsTotal: int32(placeResultItem.UserRatingsTotal), // Cast int to int32
			Types:            placeResultItem.Types,
			PhotoUrls:        placeResultItem.PhotoURLs,
		}

		// Geometry Location
		pbPlace.GeometryLocation = &pb.Place_Location{
			Lat: placeResultItem.Geometry.Location.Lat,
			Lng: placeResultItem.Geometry.Location.Lng,
		}

		// OpeningHours (simplified in pb.Place)
		// The model's OpeningHours is a struct { OpenNow bool }, matching pb.Place_OpeningHours
		pbPlace.OpeningHours = &pb.Place_OpeningHours{
			OpenNow: placeResultItem.OpeningHours.OpenNow,
		}

		// Photos
		var pbPhotos []*pb.Place_Photo
		for _, photo := range placeResultItem.Photos {
			pbPhotos = append(pbPhotos, &pb.Place_Photo{
				PhotoReference: photo.PhotoReference,
				Height:         int32(photo.Height), // Cast int to int32
				Width:          int32(photo.Width),  // Cast int to int32
			})
		}
		pbPlace.Photos = pbPhotos

		pbPlaces = append(pbPlaces, pbPlace)
	}

	return &pb.SearchPlacesResponse{
		Results: pbPlaces,
		Status:  serviceResponse.Status,
	}, nil
}

func (s *MapsServerImpl) GetPlaceDetails(ctx context.Context, req *pb.PlaceDetailsRequest) (*pb.PlaceDetailsResponse, error) {
	log.Printf("Received gRPC GetPlaceDetails request for place_id: %s", req.GetPlaceId())
	// TODO: Implement actual call to services.maps.PlacesService.FindPlaceByID() and data conversion
	return &pb.PlaceDetailsResponse{Status: "OK", Result: &pb.Place{Name: "Placeholder Place"}}, nil // Placeholder
}

func (s *MapsServerImpl) GetDirections(ctx context.Context, req *pb.DirectionsRequest) (*pb.DirectionsResponse, error) {
	log.Printf("Received gRPC GetDirections request: origin='%s', destination='%s'", req.GetOrigin(), req.GetDestination())
	// TODO: Implement actual call to services.maps.DirectionsService.GetDirections() and data conversion
	return &pb.DirectionsResponse{Status: "OK", Routes: []*pb.DirectionsResponse_Route{}}, nil // Placeholder
}

func (s *MapsServerImpl) GetDistanceMatrix(ctx context.Context, req *pb.DistanceMatrixRequest) (*pb.DistanceMatrixResponse, error) {
	log.Printf("Received gRPC GetDistanceMatrix request: origins='%s', destinations='%s'", req.GetOrigins(), req.GetDestinations())
	// TODO: Implement actual call to services.maps.DistanceMatrixService.GetDistanceMatrix() and data conversion
	return &pb.DistanceMatrixResponse{Status: "OK", Rows: []*pb.DistanceMatrixResponse_Row{}}, nil // Placeholder
}

// --- GeminiService Implementation ---
func (s *GeminiServerImpl) GenerateContent(ctx context.Context, req *pb.GeminiRequest) (*pb.GeminiResponse, error) {
	log.Printf("Received gRPC GenerateContent request for prompt: %s", req.GetPrompt())
	// TODO: Implement actual call to services.gemini.AIService.GetAIResponse() and data conversion
	// Simulate a response for now
	return &pb.GeminiResponse{
		Candidates: []*pb.GeminiResponse_Candidate{
			{
				Content: &pb.GeminiResponse_Content{
					Parts: []*pb.GeminiResponse_ContentPart{
						{Text: "This is a simulated response to: " + req.GetPrompt()},
					},
					Role: "model",
				},
				FinishReason: "STOP",
				Index: 0,
			},
		},
	}, nil
}
