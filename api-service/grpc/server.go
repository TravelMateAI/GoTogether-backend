package grpc

import (
	"context"
	"log"
	// "strconv" // For potential string to number conversions - Not used yet
	// "strings" // For potential string manipulations (e.g. lat,lng parsing) - Not used yet

	"api-service/config"    // For environment variable access
	pb "api-service/grpc/pb" // Updated generated proto package
	"api-service/models"
	"api-service/services/gemini"
	"api-service/services/maps"

	"github.com/google/generative-ai-go/genai"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// --- Service Interfaces ---
type IPlacesService interface {
	FindPlaceByID(placeID string) (*models.PlaceDetailResponse, error)
	SearchPlaces(query, location string) (*models.PlacesResponse, error)
	// Add other methods if the gRPC handlers use them, e.g. SearchPlaces
}

type IDirectionsService interface {
	GetDirections(origin, destination string) (interface{}, error)
}

type IDistanceMatrixService interface {
	GetDistanceMatrix(origins, destinations string) (*maps.DistanceMatrixResponse, error)
}

type IAIService interface {
	GetAIResponse(prompt string) (interface{}, error)
}

// --- Patchable Service Constructors ---
// These are defined to allow mocking in tests.
// By default, they use the actual service constructors.
var (
	newPlacesServiceFunc       func() IPlacesService             = func() IPlacesService { return maps.NewPlacesService() }
	newDirectionsServiceFunc   func() IDirectionsService         = func() IDirectionsService { return maps.NewDirectionsService() }
	newDistanceMatrixServiceFunc func() IDistanceMatrixService     = func() IDistanceMatrixService { return maps.NewDistanceMatrixService() }
	newAIServiceFunc           func(apiKey string) IAIService = func(apiKey string) IAIService { return gemini.NewAIService(apiKey) }
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
}

// GeminiServerImpl implements pb.GeminiServer
type GeminiServerImpl struct {
	pb.UnimplementedGeminiServer
}

// --- MapsService Implementation ---

func (s *MapsServerImpl) Geocode(ctx context.Context, req *pb.GeocodeRequest) (*pb.GeocodeResponse, error) {
	log.Printf("Received gRPC Geocode request for address: %s", req.GetAddress())

	// Note: GeocodeService is not currently using the patchable constructor pattern in this example,
	// as it was not part of the test setup. For full testability, it would follow the same pattern.
	geoService := maps.NewGeocodeService()
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

	placesService := newPlacesServiceFunc() // Use patchable constructor
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

	placesService := newPlacesServiceFunc() // Use patchable constructor
	serviceResponse, err := placesService.FindPlaceByID(req.GetPlaceId())

	if err != nil {
		log.Printf("FindPlaceByID service error: %v", err)
		return nil, status.Errorf(codes.Internal, "FindPlaceByID failed: %v", err)
	}

	if serviceResponse == nil {
		log.Printf("FindPlaceByID service returned nil response")
		return nil, status.Errorf(codes.Internal, "FindPlaceByID service returned nil response")
	}

	// Handle cases where the place is not found or another error status is returned by the service
	if serviceResponse.Status != "OK" || serviceResponse.Result == nil {
		log.Printf("FindPlaceByID service returned status: %s", serviceResponse.Status)
		return &pb.PlaceDetailsResponse{
			Status: serviceResponse.Status,
			Result: nil,
		}, nil
	}

	// Convert models.PlaceResult to pb.Place
	result := serviceResponse.Result
	pbPlace := &pb.Place{
		PlaceId:          result.PlaceID,
		Name:             result.Name,
		Rating:           result.Rating,
		UserRatingsTotal: int32(result.UserRatingsTotal),
		Types:            result.Types,
		PhotoUrls:        result.PhotoURLs, // Assuming PhotoURLs is already a []string
	}

	// Use Vicinity if available, otherwise FormattedAddress
	if result.Vicinity != "" {
		pbPlace.Vicinity = result.Vicinity
	} else {
		pbPlace.Vicinity = result.FormattedAddress
	}

	// Geometry Location
	if result.Geometry != nil && result.Geometry.Location != nil {
		pbPlace.GeometryLocation = &pb.Place_Location{
			Lat: result.Geometry.Location.Lat,
			Lng: result.Geometry.Location.Lng,
		}
	}

	// OpeningHours
	if result.OpeningHours != nil {
		pbPlace.OpeningHours = &pb.Place_OpeningHours{
			OpenNow: result.OpeningHours.OpenNow,
		}
	}

	// Photos
	var pbPhotos []*pb.Place_Photo
	for _, photo := range result.Photos {
		pbPhotos = append(pbPhotos, &pb.Place_Photo{
			PhotoReference: photo.PhotoReference,
			Height:         int32(photo.Height),
			Width:          int32(photo.Width),
		})
	}
	pbPlace.Photos = pbPhotos

	return &pb.PlaceDetailsResponse{
		Result: pbPlace,
		Status: serviceResponse.Status,
	}, nil
}

func (s *MapsServerImpl) GetDirections(ctx context.Context, req *pb.DirectionsRequest) (*pb.DirectionsResponse, error) {
	log.Printf("Received gRPC GetDirections request: origin='%s', destination='%s'", req.GetOrigin(), req.GetDestination())

	dirService := newDirectionsServiceFunc() // Use patchable constructor
	serviceResponseInterface, err := dirService.GetDirections(req.GetOrigin(), req.GetDestination())

	if err != nil {
		log.Printf("DirectionsService error: %v", err)
		return nil, status.Errorf(codes.Internal, "DirectionsService failed: %v", err)
	}

	if serviceResponseInterface == nil {
		log.Printf("DirectionsService returned nil interface")
		return nil, status.Errorf(codes.Internal, "DirectionsService returned nil interface")
	}

	serviceResponse, ok := serviceResponseInterface.(map[string]interface{})
	if !ok {
		log.Printf("DirectionsService response is not map[string]interface{}: %T", serviceResponseInterface)
		return nil, status.Errorf(codes.Internal, "DirectionsService response format error")
	}

	var responseStatus string
	if val, ok := serviceResponse["status"].(string); ok {
		responseStatus = val
	} else {
		log.Printf("DirectionsService response missing or invalid 'status' field: %v", serviceResponse)
		return nil, status.Errorf(codes.Internal, "DirectionsService response format error: missing status")
	}

	if responseStatus != "OK" {
		log.Printf("DirectionsService returned non-OK status: %s", responseStatus)
		// Return status and empty routes/waypoints if not OK
		return &pb.DirectionsResponse{Status: responseStatus}, nil
	}

	var pbRoutes []*pb.DirectionsResponse_Route
	if routesData, ok := serviceResponse["routes"].([]interface{}); ok {
		for _, routeItem := range routesData {
			routeMap, ok := routeItem.(map[string]interface{})
			if !ok {
				log.Printf("Route item is not map[string]interface{}: %T", routeItem)
				continue
			}
			pbRoute := &pb.DirectionsResponse_Route{}

			if summary, ok := routeMap["summary"].(string); ok {
				pbRoute.Summary = summary
			}

			if overviewPolylineMap, ok := routeMap["overview_polyline"].(map[string]interface{}); ok {
				if points, ok := overviewPolylineMap["points"].(string); ok {
					pbRoute.OverviewPolyline = &pb.DirectionsResponse_Polyline{Points: points}
				}
			}

			var pbLegs []*pb.DirectionsResponse_Leg
			if legsData, ok := routeMap["legs"].([]interface{}); ok {
				for _, legItem := range legsData {
					legMap, ok := legItem.(map[string]interface{})
					if !ok {
						log.Printf("Leg item is not map[string]interface{}: %T", legItem)
						continue
					}
					pbLeg := &pb.DirectionsResponse_Leg{}

					if startAddr, ok := legMap["start_address"].(string); ok {
						pbLeg.StartAddress = startAddr
					}
					if endAddr, ok := legMap["end_address"].(string); ok {
						pbLeg.EndAddress = endAddr
					}

					if distMap, ok := legMap["distance"].(map[string]interface{}); ok {
						pbDist := &pb.DirectionsResponse_Distance{}
						if text, ok := distMap["text"].(string); ok {
							pbDist.Text = text
						}
						if value, ok := distMap["value"].(float64); ok { // JSON numbers are float64
							pbDist.Value = int32(value)
						}
						pbLeg.Distance = pbDist
					}

					if durMap, ok := legMap["duration"].(map[string]interface{}); ok {
						pbDur := &pb.DirectionsResponse_Duration{}
						if text, ok := durMap["text"].(string); ok {
							pbDur.Text = text
						}
						if value, ok := durMap["value"].(float64); ok { // JSON numbers are float64
							pbDur.Value = int32(value)
						}
						pbLeg.Duration = pbDur
					}

					var pbSteps []*pb.DirectionsResponse_Step
					if stepsData, ok := legMap["steps"].([]interface{}); ok {
						for _, stepItem := range stepsData {
							stepMap, ok := stepItem.(map[string]interface{})
							if !ok {
								log.Printf("Step item is not map[string]interface{}: %T", stepItem)
								continue
							}
							pbStep := &pb.DirectionsResponse_Step{}
							if htmlInstr, ok := stepMap["html_instructions"].(string); ok {
								pbStep.HtmlInstructions = htmlInstr
							}

							if stepDistMap, ok := stepMap["distance"].(map[string]interface{}); ok {
								pbStepDist := &pb.DirectionsResponse_Distance{}
								if text, ok := stepDistMap["text"].(string); ok {
									pbStepDist.Text = text
								}
								if value, ok := stepDistMap["value"].(float64); ok {
									pbStepDist.Value = int32(value)
								}
								pbStep.Distance = pbStepDist
							}

							if stepDurMap, ok := stepMap["duration"].(map[string]interface{}); ok {
								pbStepDur := &pb.DirectionsResponse_Duration{}
								if text, ok := stepDurMap["text"].(string); ok {
									pbStepDur.Text = text
								}
								if value, ok := stepDurMap["value"].(float64); ok {
									pbStepDur.Value = int32(value)
								}
								pbStep.Duration = pbStepDur
							}

							if polylineMap, ok := stepMap["polyline"].(map[string]interface{}); ok {
								if points, ok := polylineMap["points"].(string); ok {
									pbStep.Polyline = &pb.DirectionsResponse_Polyline{Points: points}
								}
							}
							pbSteps = append(pbSteps, pbStep)
						}
					}
					pbLeg.Steps = pbSteps
					pbLegs = append(pbLegs, pbLeg)
				}
			}
			pbRoute.Legs = pbLegs
			pbRoutes = append(pbRoutes, pbRoute)
		}
	}

	var pbWaypoints []*pb.DirectionsResponse_GeocodedWaypoint
	if waypointData, ok := serviceResponse["geocoded_waypoints"].([]interface{}); ok {
		for _, waypointItem := range waypointData {
			waypointMap, ok := waypointItem.(map[string]interface{})
			if !ok {
				log.Printf("Waypoint item is not map[string]interface{}: %T", waypointItem)
				continue
			}
			pbWaypoint := &pb.DirectionsResponse_GeocodedWaypoint{}
			if geocoderStatus, ok := waypointMap["geocoder_status"].(string); ok {
				pbWaypoint.GeocoderStatus = geocoderStatus
			}
			if placeID, ok := waypointMap["place_id"].(string); ok {
				pbWaypoint.PlaceId = placeID
			}
			if typesInterface, ok := waypointMap["types"].([]interface{}); ok {
				var typesStr []string
				for _, typeItem := range typesInterface {
					if typeStr, ok := typeItem.(string); ok {
						typesStr = append(typesStr, typeStr)
					}
				}
				pbWaypoint.Types = typesStr
			}
			pbWaypoints = append(pbWaypoints, pbWaypoint)
		}
	}

	return &pb.DirectionsResponse{
		Status:            responseStatus,
		Routes:            pbRoutes,
		GeocodedWaypoints: pbWaypoints,
	}, nil
}

func (s *MapsServerImpl) GetDistanceMatrix(ctx context.Context, req *pb.DistanceMatrixRequest) (*pb.DistanceMatrixResponse, error) {
	log.Printf("Received gRPC GetDistanceMatrix request: origins='%s', destinations='%s'", req.GetOrigins(), req.GetDestinations())

	distMatrixService := newDistanceMatrixServiceFunc() // Use patchable constructor
	serviceResponse, err := distMatrixService.GetDistanceMatrix(req.GetOrigins(), req.GetDestinations())

	if err != nil {
		log.Printf("DistanceMatrixService error: %v", err)
		return nil, status.Errorf(codes.Internal, "DistanceMatrixService failed: %v", err)
	}

	if serviceResponse == nil {
		log.Printf("DistanceMatrixService returned nil response")
		return nil, status.Errorf(codes.Internal, "DistanceMatrixService returned nil response")
	}

	// Convert maps.DistanceMatrixResponse to pb.DistanceMatrixResponse
	pbResp := &pb.DistanceMatrixResponse{
		OriginAddresses:      serviceResponse.OriginAddresses,
		DestinationAddresses: serviceResponse.DestinationAddresses,
		Status:               serviceResponse.Status, // Assuming maps.DistanceMatrixResponse has Status
	}

	var pbRows []*pb.DistanceMatrixResponse_Row
	for _, serviceRow := range serviceResponse.Rows {
		pbRow := &pb.DistanceMatrixResponse_Row{}
		var pbElements []*pb.DistanceMatrixResponse_Element
		for _, serviceElement := range serviceRow.Elements {
			pbElement := &pb.DistanceMatrixResponse_Element{
				Status: serviceElement.Status,
			}
			if serviceElement.Duration != nil {
				pbElement.Duration = &pb.DistanceMatrixResponse_Element_Duration{
					Text:  serviceElement.Duration.Text,
					Value: int32(serviceElement.Duration.Value),
				}
			}
			if serviceElement.Distance != nil {
				pbElement.Distance = &pb.DistanceMatrixResponse_Element_Distance{
					Text:  serviceElement.Distance.Text,
					Value: int32(serviceElement.Distance.Value),
				}
			}
			pbElements = append(pbElements, pbElement)
		}
		pbRow.Elements = pbElements
		pbRows = append(pbRows, pbRow)
	}
	pbResp.Rows = pbRows

	// If overall status is not OK, log it. The status is already set in pbResp.
	if serviceResponse.Status != "OK" {
		log.Printf("DistanceMatrixService returned non-OK status: %s", serviceResponse.Status)
	}

	return pbResp, nil
}

// --- GeminiService Implementation ---
func (s *GeminiServerImpl) GenerateContent(ctx context.Context, req *pb.GeminiRequest) (*pb.GeminiResponse, error) {
	log.Printf("Received gRPC GenerateContent request for prompt (first 100 chars): %.100s", req.GetPrompt())

	apiKey := config.GetEnv("GOOGLE_GEMINI_API_KEY", "")
	if apiKey == "" {
		log.Printf("GOOGLE_GEMINI_API_KEY not set in environment")
		return nil, status.Errorf(codes.FailedPrecondition, "GOOGLE_GEMINI_API_KEY not configured")
	}

	aiService := newAIServiceFunc(apiKey) // Use patchable constructor
	serviceResponseInterface, err := aiService.GetAIResponse(req.GetPrompt())

	if err != nil {
		log.Printf("Gemini AIService error: %v", err)
		return nil, status.Errorf(codes.Internal, "Gemini AIService failed: %v", err)
	}

	if serviceResponseInterface == nil {
		log.Printf("Gemini AIService returned nil interface")
		return nil, status.Errorf(codes.Internal, "Gemini AIService returned nil interface")
	}

	genaiResponse, ok := serviceResponseInterface.(*genai.GenerateContentResponse)
	if !ok {
		log.Printf("Gemini AIService response is not *genai.GenerateContentResponse: %T", serviceResponseInterface)
		return nil, status.Errorf(codes.Internal, "Gemini AIService response format error")
	}

	pbResp := &pb.GeminiResponse{}

	// Candidates
	for _, candidate := range genaiResponse.Candidates {
		pbCandidate := &pb.GeminiResponse_Candidate{
			FinishReason: "", // Default, will be overwritten
			Index:        int32(candidate.Index),
		}
		if candidate.FinishReason > 0 { // Check if FinishReason is valid before converting
			pbCandidate.FinishReason = candidate.FinishReason.String()
		}


		// Content
		if candidate.Content != nil {
			pbContent := &pb.GeminiResponse_Content{Role: candidate.Content.Role}
			for _, part := range candidate.Content.Parts {
				if textPart, ok := part.(genai.Text); ok {
					pbContent.Parts = append(pbContent.Parts, &pb.GeminiResponse_ContentPart{Text: string(textPart)})
				} else {
					log.Printf("Unhandled part type in candidate content: %T", part)
				}
			}
			pbCandidate.Content = pbContent
		}

		// SafetyRatings
		for _, safetyRating := range candidate.SafetyRatings {
			pbSafetyRating := &pb.GeminiResponse_SafetyRating{
				Category:    safetyRating.Category.String(),
				Probability: safetyRating.Probability.String(),
			}
			pbCandidate.SafetyRatings = append(pbCandidate.SafetyRatings, pbSafetyRating)
		}
		pbResp.Candidates = append(pbResp.Candidates, pbCandidate)
	}

	// PromptFeedback
	if genaiResponse.PromptFeedback != nil {
		pbPromptFeedback := &pb.GeminiResponse_PromptFeedback{}
		for _, safetyRating := range genaiResponse.PromptFeedback.SafetyRatings {
			pbSafetyRating := &pb.GeminiResponse_SafetyRating{
				Category:    safetyRating.Category.String(),
				Probability: safetyRating.Probability.String(),
			}
			pbPromptFeedback.SafetyRatings = append(pbPromptFeedback.SafetyRatings, pbSafetyRating)
		}
		// Note: PromptFeedback.BlockReason is not in pb.GeminiResponse.PromptFeedback
		pbResp.PromptFeedback = pbPromptFeedback
	}

	return pbResp, nil
}
