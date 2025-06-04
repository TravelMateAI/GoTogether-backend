package maps

import (
	"api-service/config"
	"api-service/models"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type PlacesService struct {
	client *http.Client
	apiKey string
}

func NewPlacesService() *PlacesService {
	return &PlacesService{
		client: &http.Client{Timeout: 10 * time.Second},
		apiKey: "",
	}
}

func (s *PlacesService) FindPlaceByID(placeID string) (*models.PlaceDetailResponse, error) {
	var response models.PlaceDetailResponse // Initialize response struct

	fields := "name,rating,formatted_phone_number,photo,review,website,opening_hours,geometry,place_id,type,vicinity,address_component,utc_offset,user_ratings_total,formatted_address,international_phone_number"
	url := fmt.Sprintf("https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&fields=%s&key=%s", placeID, fields, config.GetEnv("GOOGLE_MAPS_API_KEY", ""))

	resp, err := s.client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	decodeErr := json.NewDecoder(resp.Body).Decode(&response)

	if resp.StatusCode != http.StatusOK {
		if decodeErr != nil {
			return nil, fmt.Errorf("failed to get place details: status %s. Additionally, failed to decode error response body: %v", resp.Status, decodeErr)
		}
		// Use response.Status and response.ErrorMessage from the decoded Google error response
		return &response, fmt.Errorf("failed to get place details: Google API status %s, message: %s", response.Status, response.ErrorMessage)
	}

	if decodeErr != nil {
		return nil, fmt.Errorf("failed to decode successful place details response: %v", decodeErr)
	}

	// Populate PhotoURLs if Photos exist and status is OK
	if response.Status == "OK" {
		if response.Result.Photos != nil && len(response.Result.Photos) > 0 {
			response.Result.PhotoURLs = []string{} // Initialize PhotoURLs
			apiKey := config.GetEnv("GOOGLE_MAPS_API_KEY", "")
			for _, photo := range response.Result.Photos {
				if photo.PhotoReference != "" {
					photoURL := fmt.Sprintf("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=%s&key=%s", photo.PhotoReference, apiKey)
					response.Result.PhotoURLs = append(response.Result.PhotoURLs, photoURL)
				}
			}
		}
	}

	return &response, nil
}

func (s *PlacesService) NearbySearch(location string, radius int) (*models.PlacesResponse, error) {
	url := fmt.Sprintf("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%s&radius=%d&key=%s", location, radius, config.GetEnv("GOOGLE_MAPS_API_KEY", ""))
	resp, err := s.client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to search nearby places: %s", resp.Status)
	}

	var response models.PlacesResponse
	if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
		return nil, err
	}

	apiKey := config.GetEnv("GOOGLE_MAPS_API_KEY", "")
	for i := range response.Results {
		response.Results[i].PhotoURLs = []string{} // Initialize
		if response.Results[i].Photos != nil {
			for _, photo := range response.Results[i].Photos {
				if photo.PhotoReference != "" {
					photoURL := fmt.Sprintf("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=%s&key=%s", photo.PhotoReference, apiKey)
					response.Results[i].PhotoURLs = append(response.Results[i].PhotoURLs, photoURL)
				}
			}
		}
	}
	return &response, nil
}

func (s *PlacesService) TextSearch(query string) (*models.PlacesResponse, error) {
	url := fmt.Sprintf("https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&key=%s", query, config.GetEnv("GOOGLE_MAPS_API_KEY", ""))
	resp, err := s.client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to search places: %s", resp.Status)
	}

	var response models.PlacesResponse
	if err := json.NewDecoder(resp.Body).Decode(&response); err != nil {
		return nil, err
	}

	apiKey := config.GetEnv("GOOGLE_MAPS_API_KEY", "")
	for i := range response.Results {
		response.Results[i].PhotoURLs = []string{} // Initialize
		if response.Results[i].Photos != nil {
			for _, photo := range response.Results[i].Photos {
				if photo.PhotoReference != "" {
					photoURL := fmt.Sprintf("https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=%s&key=%s", photo.PhotoReference, apiKey)
					response.Results[i].PhotoURLs = append(response.Results[i].PhotoURLs, photoURL)
				}
			}
		}
	}
	return &response, nil
}

// SearchPlaces combines text and nearby search based on provided parameters
func (s *PlacesService) SearchPlaces(query, location string) (*models.PlacesResponse, error) {
	if query != "" {
		return s.TextSearch(query)
	} else if location != "" {
		// Default radius for nearby search (in meters)
		return s.NearbySearch(location, 1000)
	}
	return nil, fmt.Errorf("either query or location must be provided")
}
