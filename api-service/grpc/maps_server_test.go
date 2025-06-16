package grpc

import (
	"context"
	"errors"
	"reflect"
	"testing"

	"api-service/models"
	"api-service/services/maps" // For maps.DistanceMatrixResponse etc.
	pb_maps "api-service/grpc/pb/maps"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// --- Mock Definitions for Maps ---

// MockGeocodeService for IGeocodeService
type MockGeocodeService struct {
	GeocodeFunc func(address string) (map[string]interface{}, error)
}

func (m *MockGeocodeService) Geocode(address string) (map[string]interface{}, error) {
	if m.GeocodeFunc != nil {
		return m.GeocodeFunc(address)
	}
	return nil, errors.New("GeocodeFunc not implemented in mock")
}

// MockPlacesService for IPlacesService
type MockPlacesService struct {
	FindPlaceByIDFunc func(placeID string) (*models.PlaceDetailResponse, error)
	SearchPlacesFunc  func(query, location string) (*models.PlacesResponse, error)
}

func (m *MockPlacesService) FindPlaceByID(placeID string) (*models.PlaceDetailResponse, error) {
	if m.FindPlaceByIDFunc != nil {
		return m.FindPlaceByIDFunc(placeID)
	}
	return nil, errors.New("FindPlaceByIDFunc not implemented in mock")
}

func (m *MockPlacesService) SearchPlaces(query, location string) (*models.PlacesResponse, error) {
	if m.SearchPlacesFunc != nil {
		return m.SearchPlacesFunc(query, location)
	}
	return nil, errors.New("SearchPlacesFunc not implemented in mock")
}

// MockDirectionsService for IDirectionsService
type MockDirectionsService struct {
	GetDirectionsFunc func(origin, destination string) (interface{}, error)
}

func (m *MockDirectionsService) GetDirections(origin, destination string) (interface{}, error) {
	if m.GetDirectionsFunc != nil {
		return m.GetDirectionsFunc(origin, destination)
	}
	return nil, errors.New("GetDirectionsFunc not implemented in mock")
}

// MockDistanceMatrixService for IDistanceMatrixService
type MockDistanceMatrixService struct {
	GetDistanceMatrixFunc func(origins, destinations string) (*maps.DistanceMatrixResponse, error)
}

func (m *MockDistanceMatrixService) GetDistanceMatrix(origins, destinations string) (*maps.DistanceMatrixResponse, error) {
	if m.GetDistanceMatrixFunc != nil {
		return m.GetDistanceMatrixFunc(origins, destinations)
	}
	return nil, errors.New("GetDistanceMatrixFunc not implemented in mock")
}


// --- Test GetPlaceDetails ---
func TestMapsServerImpl_GetPlaceDetails(t *testing.T) {
	server := &MapsServerImpl{} // Uses the Impl from maps_server.go
	ctx := context.Background()

	// Original newPlacesServiceFunc from maps_server.go
	originalFunc := newPlacesServiceFunc
	defer func() { newPlacesServiceFunc = originalFunc }()

	mockService := &MockPlacesService{}
	// Patch the constructor function in maps_server.go
	newPlacesServiceFunc = func() IPlacesService { return mockService }

	tests := []struct {
		name         string
		req          *pb_maps.PlaceDetailsRequest
		mockSetup    func()
		expectedResp *pb_maps.PlaceDetailsResponse
		expectedCode codes.Code
	}{
		{
			name: "Success",
			req:  &pb_maps.PlaceDetailsRequest{PlaceId: "test_place_id"},
			mockSetup: func() {
				mockService.FindPlaceByIDFunc = func(placeID string) (*models.PlaceDetailResponse, error) {
					return &models.PlaceDetailResponse{
						Status: "OK",
						Result: &models.PlaceResult{
							PlaceID:          "test_place_id",
							Name:             "Test Place",
							Vicinity:         "Test Vicinity",
							Rating:           4.5,
							UserRatingsTotal: 100,
							Types:            []string{"cafe", "bakery"},
							Geometry:         &models.Geometry{Location: &models.Location{Lat: 10.0, Lng: 20.0}},
							OpeningHours:     &models.OpeningHours{OpenNow: true},
							Photos:           []models.Photo{{PhotoReference: "ref1", Height: 100, Width: 200}},
							PhotoURLs:        []string{"http://example.com/photo1.jpg"},
						},
					}, nil
				}
			},
			expectedResp: &pb_maps.PlaceDetailsResponse{
				Status: "OK",
				Result: &pb_maps.Place{
					PlaceId:          "test_place_id",
					Name:             "Test Place",
					Vicinity:         "Test Vicinity",
					Rating:           4.5,
					UserRatingsTotal: 100,
					Types:            []string{"cafe", "bakery"},
					GeometryLocation: &pb_maps.Place_Location{Lat: 10.0, Lng: 20.0},
					OpeningHours:     &pb_maps.Place_OpeningHours{OpenNow: true},
					Photos:           []*pb_maps.Place_Photo{{PhotoReference: "ref1", Height: 100, Width: 200}},
					PhotoUrls:        []string{"http://example.com/photo1.jpg"},
				},
			},
			expectedCode: codes.OK,
		},
		{
			name: "Service Error",
			req:  &pb_maps.PlaceDetailsRequest{PlaceId: "error_id"},
			mockSetup: func() {
				mockService.FindPlaceByIDFunc = func(placeID string) (*models.PlaceDetailResponse, error) {
					return nil, errors.New("internal service error")
				}
			},
			expectedResp: nil,
			expectedCode: codes.Internal,
		},
		{
			name: "Place Not Found (NOT_FOUND status)",
			req:  &pb_maps.PlaceDetailsRequest{PlaceId: "not_found_id"},
			mockSetup: func() {
				mockService.FindPlaceByIDFunc = func(placeID string) (*models.PlaceDetailResponse, error) {
					return &models.PlaceDetailResponse{Status: "NOT_FOUND", Result: nil}, nil
				}
			},
			expectedResp: &pb_maps.PlaceDetailsResponse{Status: "NOT_FOUND", Result: nil},
			expectedCode: codes.OK,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tt.mockSetup()
			resp, err := server.GetPlaceDetails(ctx, tt.req)

			if tt.expectedCode != codes.OK {
				if err == nil {
					t.Fatalf("Expected error code %v, got nil", tt.expectedCode)
				}
				st, ok := status.FromError(err)
				if !ok || st.Code() != tt.expectedCode {
					t.Fatalf("Expected gRPC status code %v, got %v (err: %v)", tt.expectedCode, st.Code(), err)
				}
			} else {
				if err != nil {
					t.Fatalf("Expected no error, got %v", err)
				}
				if !reflect.DeepEqual(resp, tt.expectedResp) {
					t.Errorf("Response mismatch:\nGot: %+v\nWant:%+v", resp, tt.expectedResp)
				}
			}
		})
	}
}

// TODO: Add tests for Geocode, SearchPlaces, GetDirections, GetDistanceMatrix
// using a similar pattern of mocking their respective newXServiceFunc variables
// and testing success/error cases.
// For example, for Geocode:
// originalGeocodeFunc := newGeocodeServiceFunc
// defer func() { newGeocodeServiceFunc = originalGeocodeFunc }()
// mockGeoSvc := &MockGeocodeService{}
// newGeocodeServiceFunc = func() IGeocodeService { return mockGeoSvc }
// ... then setup mockGeoSvc.GeocodeFunc ...
// server.Geocode(...)
// ... assertions ...
