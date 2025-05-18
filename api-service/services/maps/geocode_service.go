package maps

import (
	"api-service/config"
	"encoding/json"
	"fmt"
	"net/http"
)

type GeocodeService struct {
	apiKey string
}

func NewGeocodeService() *GeocodeService {
	return &GeocodeService{apiKey: ""}
}

func (g *GeocodeService) Geocode(address string) (map[string]interface{}, error) {
	url := fmt.Sprintf("https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s", address, config.GetEnv("GOOGLE_MAPS_API_KEY", ""))

	resp, err := http.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to get geocode: %s", resp.Status)
	}

	var result map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, err
	}

	return result, nil
}
