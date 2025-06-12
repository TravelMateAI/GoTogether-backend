package maps

import (
	"api-service/config"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type DirectionsService struct {
	client *http.Client
}

func NewDirectionsService() *DirectionsService {
	return &DirectionsService{
		client: &http.Client{Timeout: 10 * time.Second},
	}
}

func (ds *DirectionsService) GetDirections(origin, destination string) (interface{}, error) {
	url := fmt.Sprintf("https://maps.googleapis.com/maps/api/directions/json?origin=%s&destination=%s&key=%s", origin, destination, config.GetEnv("GOOGLE_MAPS_API_KEY", ""))

	resp, err := ds.client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to get directions: %s", resp.Status)
	}

	var result interface{}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}

	return result, nil
}
