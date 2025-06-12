package maps

import (
	"api-service/config"
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

func (s *PlacesService) FindPlaceByID(placeID string) (map[string]interface{}, error) {
	url := fmt.Sprintf("https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&key=%s", placeID, config.GetEnv("GOOGLE_MAPS_API_KEY", ""))
	resp, err := s.client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to get place details: %s", resp.Status)
	}

	var result map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}

	return result, nil
}

func (s *PlacesService) NearbySearch(location string, radius int) (map[string]interface{}, error) {
	url := fmt.Sprintf("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%s&radius=%d&key=%s", location, radius, config.GetEnv("GOOGLE_MAPS_API_KEY", ""))
	resp, err := s.client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to search nearby places: %s", resp.Status)
	}

	var result map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}

	return result, nil
}

func (s *PlacesService) TextSearch(query string) (map[string]interface{}, error) {
	url := fmt.Sprintf("https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&key=%s", query, config.GetEnv("GOOGLE_MAPS_API_KEY", ""))
	resp, err := s.client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to search places: %s", resp.Status)
	}

	var result map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}

	return result, nil
}

// SearchPlaces combines text and nearby search based on provided parameters
func (s *PlacesService) SearchPlaces(query, location string) (map[string]interface{}, error) {
	if query != "" {
		return s.TextSearch(query)
	} else if location != "" {
		// Default radius for nearby search (in meters)
		return s.NearbySearch(location, 1000)
	}
	return nil, fmt.Errorf("either query or location must be provided")
}
