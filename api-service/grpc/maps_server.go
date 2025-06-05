package grpc

import (
	"context"
	"log"

	"api-service/models"
	"api-service/services/maps"
	pb_maps "api-service/grpc/pb/maps" // Alias for the new maps protobuf package

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// --- Service Interfaces for Maps ---
type IGeocodeService interface {
	Geocode(address string) (map[string]interface{}, error)
}

type IPlacesService interface {
	FindPlaceByID(placeID string) (*models.PlaceDetailResponse, error)
	SearchPlaces(query, location string) (*models.PlacesResponse, error)
}

type IDirectionsService interface {
	GetDirections(origin, destination string) (interface{}, error)
}

type IDistanceMatrixService interface {
	GetDistanceMatrix(origins, destinations string) (*maps.DistanceMatrixResponse, error)
}

// --- Patchable Service Constructors for Maps ---
var (
	newGeocodeServiceFunc      func() IGeocodeService             = func() IGeocodeService { return maps.NewGeocodeService() }
	newPlacesServiceFunc       func() IPlacesService              = func() IPlacesService { return maps.NewPlacesService() }
	newDirectionsServiceFunc   func() IDirectionsService          = func() IDirectionsService { return maps.NewDirectionsService() }
	newDistanceMatrixServiceFunc func() IDistanceMatrixService    = func() IDistanceMatrixService { return maps.NewDistanceMatrixService() }
)

// MapsServerImpl implements pb_maps.MapsServer
type MapsServerImpl struct {
	pb_maps.UnimplementedMapsServer
}

// --- MapsService Implementation ---

func (s *MapsServerImpl) Geocode(ctx context.Context, req *pb_maps.GeocodeRequest) (*pb_maps.GeocodeResponse, error) {
	log.Printf("Received gRPC Geocode request for address: %s", req.GetAddress())

	geoService := newGeocodeServiceFunc() // Use patchable constructor
	serviceResponse, err := geoService.Geocode(req.GetAddress())

	if err != nil {
		log.Printf("Geocode service error: %v", err)
		return nil, status.Errorf(codes.Internal, "Geocoding failed: %v", err)
	}

	var pbResults []*pb_maps.GeocodeResponse_Result
	var responseStatus string
	if val, ok := serviceResponse["status"].(string); ok {
		responseStatus = val
	} else {
		log.Printf("Geocode service response missing or invalid 'status' field: %v", serviceResponse)
		return nil, status.Errorf(codes.Internal, "Geocoding response format error: missing status")
	}

	if responseStatus != "OK" && responseStatus != "ZERO_RESULTS" {
		log.Printf("Geocode service returned non-OK status: %s", responseStatus)
	}

	if resultsData, ok := serviceResponse["results"].([]interface{}); ok {
		for _, item := range resultsData {
			if resultMap, ok := item.(map[string]interface{}); ok {
				pbRes := &pb_maps.GeocodeResponse_Result{}
				if formattedAddr, ok := resultMap["formatted_address"].(string); ok {
					pbRes.FormattedAddress = formattedAddr
				}
				if geometry, ok := resultMap["geometry"].(map[string]interface{}); ok {
					if location, ok := geometry["location"].(map[string]interface{}); ok {
						lat, latOk := location["lat"].(float64)
						lng, lngOk := location["lng"].(float64)
						if latOk && lngOk {
							pbRes.Location = &pb_maps.GeocodeResponse_Location{Lat: lat, Lng: lng}
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

	return &pb_maps.GeocodeResponse{
		Results: pbResults,
		Status:  responseStatus,
	}, nil
}

func (s *MapsServerImpl) SearchPlaces(ctx context.Context, req *pb_maps.SearchPlacesRequest) (*pb_maps.SearchPlacesResponse, error) {
	log.Printf("Received gRPC SearchPlaces request: query='%s', location='%s'", req.GetQuery(), req.GetLocation())

	placesService := newPlacesServiceFunc()
	serviceResponse, err := placesService.SearchPlaces(req.GetQuery(), req.GetLocation())

	if err != nil {
		log.Printf("SearchPlaces service error: %v", err)
		return nil, status.Errorf(codes.Internal, "SearchPlaces failed: %v", err)
	}
	if serviceResponse == nil {
		log.Printf("SearchPlaces service returned nil response")
		return nil, status.Errorf(codes.Internal, "SearchPlaces service returned nil response")
	}

	var pbPlaces []*pb_maps.Place
	for _, placeResultItem := range serviceResponse.Results {
		pbPlace := &pb_maps.Place{
			PlaceId:          placeResultItem.PlaceID,
			Name:             placeResultItem.Name,
			Vicinity:         placeResultItem.Address,
			Rating:           placeResultItem.Rating,
			UserRatingsTotal: int32(placeResultItem.UserRatingsTotal),
			Types:            placeResultItem.Types,
			PhotoUrls:        placeResultItem.PhotoURLs,
		}
		if placeResultItem.Geometry != nil && placeResultItem.Geometry.Location != nil {
			pbPlace.GeometryLocation = &pb_maps.Place_Location{
				Lat: placeResultItem.Geometry.Location.Lat,
				Lng: placeResultItem.Geometry.Location.Lng,
			}
		}
		if placeResultItem.OpeningHours != nil {
			pbPlace.OpeningHours = &pb_maps.Place_OpeningHours{
				OpenNow: placeResultItem.OpeningHours.OpenNow,
			}
		}
		var pbPhotos []*pb_maps.Place_Photo
		for _, photo := range placeResultItem.Photos {
			pbPhotos = append(pbPhotos, &pb_maps.Place_Photo{
				PhotoReference: photo.PhotoReference,
				Height:         int32(photo.Height),
				Width:          int32(photo.Width),
			})
		}
		pbPlace.Photos = pbPhotos
		pbPlaces = append(pbPlaces, pbPlace)
	}

	return &pb_maps.SearchPlacesResponse{
		Results: pbPlaces,
		Status:  serviceResponse.Status,
	}, nil
}

func (s *MapsServerImpl) GetPlaceDetails(ctx context.Context, req *pb_maps.PlaceDetailsRequest) (*pb_maps.PlaceDetailsResponse, error) {
	log.Printf("Received gRPC GetPlaceDetails request for place_id: %s", req.GetPlaceId())

	placesService := newPlacesServiceFunc()
	serviceResponse, err := placesService.FindPlaceByID(req.GetPlaceId())

	if err != nil {
		log.Printf("FindPlaceByID service error: %v", err)
		return nil, status.Errorf(codes.Internal, "FindPlaceByID failed: %v", err)
	}
	if serviceResponse == nil {
		log.Printf("FindPlaceByID service returned nil response")
		return nil, status.Errorf(codes.Internal, "FindPlaceByID service returned nil response")
	}

	if serviceResponse.Status != "OK" || serviceResponse.Result == nil {
		log.Printf("FindPlaceByID service returned status: %s", serviceResponse.Status)
		return &pb_maps.PlaceDetailsResponse{
			Status: serviceResponse.Status,
			Result: nil,
		}, nil
	}

	result := serviceResponse.Result
	pbPlace := &pb_maps.Place{
		PlaceId:          result.PlaceID,
		Name:             result.Name,
		Rating:           result.Rating,
		UserRatingsTotal: int32(result.UserRatingsTotal),
		Types:            result.Types,
		PhotoUrls:        result.PhotoURLs,
	}
	if result.Vicinity != "" {
		pbPlace.Vicinity = result.Vicinity
	} else {
		pbPlace.Vicinity = result.FormattedAddress
	}
	if result.Geometry != nil && result.Geometry.Location != nil {
		pbPlace.GeometryLocation = &pb_maps.Place_Location{
			Lat: result.Geometry.Location.Lat,
			Lng: result.Geometry.Location.Lng,
		}
	}
	if result.OpeningHours != nil {
		pbPlace.OpeningHours = &pb_maps.Place_OpeningHours{
			OpenNow: result.OpeningHours.OpenNow,
		}
	}
	var pbPhotos []*pb_maps.Place_Photo
	for _, photo := range result.Photos {
		pbPhotos = append(pbPhotos, &pb_maps.Place_Photo{
			PhotoReference: photo.PhotoReference,
			Height:         int32(photo.Height),
			Width:          int32(photo.Width),
		})
	}
	pbPlace.Photos = pbPhotos

	return &pb_maps.PlaceDetailsResponse{
		Result: pbPlace,
		Status: serviceResponse.Status,
	}, nil
}

func (s *MapsServerImpl) GetDirections(ctx context.Context, req *pb_maps.DirectionsRequest) (*pb_maps.DirectionsResponse, error) {
	log.Printf("Received gRPC GetDirections request: origin='%s', destination='%s'", req.GetOrigin(), req.GetDestination())

	dirService := newDirectionsServiceFunc()
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
		return &pb_maps.DirectionsResponse{Status: responseStatus}, nil
	}

	var pbRoutes []*pb_maps.DirectionsResponse_Route
	if routesData, ok := serviceResponse["routes"].([]interface{}); ok {
		for _, routeItem := range routesData {
			routeMap, rok := routeItem.(map[string]interface{})
			if !rok { continue }
			pbRoute := &pb_maps.DirectionsResponse_Route{}
			if summary, ok := routeMap["summary"].(string); ok { pbRoute.Summary = summary }
			if opm, ok := routeMap["overview_polyline"].(map[string]interface{}); ok {
				if pts, ok := opm["points"].(string); ok { pbRoute.OverviewPolyline = &pb_maps.DirectionsResponse_Polyline{Points: pts} }
			}

			var pbLegs []*pb_maps.DirectionsResponse_Leg
			if legsData, ok := routeMap["legs"].([]interface{}); ok {
				for _, legItem := range legsData {
					legMap, lok := legItem.(map[string]interface{})
					if !lok { continue }
					pbLeg := &pb_maps.DirectionsResponse_Leg{}
					if sa, ok := legMap["start_address"].(string); ok { pbLeg.StartAddress = sa }
					if ea, ok := legMap["end_address"].(string); ok { pbLeg.EndAddress = ea }
					if distMap, ok := legMap["distance"].(map[string]interface{}); ok {
						pbDist := &pb_maps.DirectionsResponse_Distance{}
						if txt, ok := distMap["text"].(string); ok { pbDist.Text = txt }
						if val, ok := distMap["value"].(float64); ok { pbDist.Value = int32(val) }
						pbLeg.Distance = pbDist
					}
					if durMap, ok := legMap["duration"].(map[string]interface{}); ok {
						pbDur := &pb_maps.DirectionsResponse_Duration{}
						if txt, ok := durMap["text"].(string); ok { pbDur.Text = txt }
						if val, ok := durMap["value"].(float64); ok { pbDur.Value = int32(val) }
						pbLeg.Duration = pbDur
					}

					var pbSteps []*pb_maps.DirectionsResponse_Step
					if stepsData, ok := legMap["steps"].([]interface{}); ok {
						for _, stepItem := range stepsData {
							stepMap, sok := stepItem.(map[string]interface{})
							if !sok { continue }
							pbStep := &pb_maps.DirectionsResponse_Step{}
							if hi, ok := stepMap["html_instructions"].(string); ok { pbStep.HtmlInstructions = hi }
							if stepDistMap, ok := stepMap["distance"].(map[string]interface{}); ok {
								pbStepDist := &pb_maps.DirectionsResponse_Distance{}
								if txt, ok := stepDistMap["text"].(string); ok { pbStepDist.Text = txt }
								if val, ok := stepDistMap["value"].(float64); ok { pbStepDist.Value = int32(val) }
								pbStep.Distance = pbStepDist
							}
							if stepDurMap, ok := stepMap["duration"].(map[string]interface{}); ok {
								pbStepDur := &pb_maps.DirectionsResponse_Duration{}
								if txt, ok := stepDurMap["text"].(string); ok { pbStepDur.Text = txt }
								if val, ok := stepDurMap["value"].(float64); ok { pbStepDur.Value = int32(val) }
								pbStep.Duration = pbStepDur
							}
							if polyMap, ok := stepMap["polyline"].(map[string]interface{}); ok {
								if pts, ok := polyMap["points"].(string); ok { pbStep.Polyline = &pb_maps.DirectionsResponse_Polyline{Points: pts} }
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

	var pbWaypoints []*pb_maps.DirectionsResponse_GeocodedWaypoint
	if waypointData, ok := serviceResponse["geocoded_waypoints"].([]interface{}); ok {
		for _, waypointItem := range waypointData {
			waypointMap, wok := waypointItem.(map[string]interface{})
			if !wok { continue }
			pbWaypoint := &pb_maps.DirectionsResponse_GeocodedWaypoint{}
			if gs, ok := waypointMap["geocoder_status"].(string); ok { pbWaypoint.GeocoderStatus = gs }
			if pid, ok := waypointMap["place_id"].(string); ok { pbWaypoint.PlaceId = pid }
			if typesInterface, ok := waypointMap["types"].([]interface{}); ok {
				var typesStr []string
				for _, typeItem := range typesInterface {
					if typeStr, ok := typeItem.(string); ok { typesStr = append(typesStr, typeStr) }
				}
				pbWaypoint.Types = typesStr
			}
			pbWaypoints = append(pbWaypoints, pbWaypoint)
		}
	}

	return &pb_maps.DirectionsResponse{
		Status:            responseStatus,
		Routes:            pbRoutes,
		GeocodedWaypoints: pbWaypoints,
	}, nil
}

func (s *MapsServerImpl) GetDistanceMatrix(ctx context.Context, req *pb_maps.DistanceMatrixRequest) (*pb_maps.DistanceMatrixResponse, error) {
	log.Printf("Received gRPC GetDistanceMatrix request: origins='%s', destinations='%s'", req.GetOrigins(), req.GetDestinations())

	distMatrixService := newDistanceMatrixServiceFunc()
	serviceResponse, err := distMatrixService.GetDistanceMatrix(req.GetOrigins(), req.GetDestinations())

	if err != nil {
		log.Printf("DistanceMatrixService error: %v", err)
		return nil, status.Errorf(codes.Internal, "DistanceMatrixService failed: %v", err)
	}
	if serviceResponse == nil {
		log.Printf("DistanceMatrixService returned nil response")
		return nil, status.Errorf(codes.Internal, "DistanceMatrixService returned nil response")
	}

	pbResp := &pb_maps.DistanceMatrixResponse{
		OriginAddresses:      serviceResponse.OriginAddresses,
		DestinationAddresses: serviceResponse.DestinationAddresses,
		Status:               serviceResponse.Status,
	}

	var pbRows []*pb_maps.DistanceMatrixResponse_Row
	for _, serviceRow := range serviceResponse.Rows {
		pbRow := &pb_maps.DistanceMatrixResponse_Row{}
		var pbElements []*pb_maps.DistanceMatrixResponse_Element
		for _, serviceElement := range serviceRow.Elements {
			pbElement := &pb_maps.DistanceMatrixResponse_Element{Status: serviceElement.Status}
			if serviceElement.Duration != nil {
				pbElement.Duration = &pb_maps.DistanceMatrixResponse_Element_Duration{
					Text:  serviceElement.Duration.Text,
					Value: int32(serviceElement.Duration.Value),
				}
			}
			if serviceElement.Distance != nil {
				pbElement.Distance = &pb_maps.DistanceMatrixResponse_Element_Distance{
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

	if serviceResponse.Status != "OK" {
		log.Printf("DistanceMatrixService returned non-OK status: %s", serviceResponse.Status)
	}
	return pbResp, nil
}
