package maps

import (
	"api-service/config"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type DistanceMatrixService struct {
	client *http.Client
}

type DistanceMatrixResponse struct {
	Rows []DistanceMatrixRow `json:"rows"`
}

type DistanceMatrixRow struct {
	Elements []DistanceMatrixElement `json:"elements"`
}

type DistanceMatrixElement struct {
	Status   string `json:"status"`
	Duration struct {
		Text  string `json:"text"`
		Value int    `json:"value"`
	} `json:"duration"`
	Distance struct {
		Text  string `json:"text"`
		Value int    `json:"value"`
	} `json:"distance"`
}

func NewDistanceMatrixService() *DistanceMatrixService {
	return &DistanceMatrixService{
		client: &http.Client{Timeout: 10 * time.Second},
	}
}

func (d *DistanceMatrixService) GetDistanceMatrix(origins, destinations string) (*DistanceMatrixResponse, error) {
	url := fmt.Sprintf("https://maps.googleapis.com/maps/api/distancematrix/json?origins=%s&destinations=%s&key=%s", origins, destinations, config.GetEnv("GOOGLE_MAPS_API_KEY", ""))

	resp, err := d.client.Get(url)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("failed to get distance matrix: %s", resp.Status)
	}

	var distanceMatrixResponse DistanceMatrixResponse
	if err := json.NewDecoder(resp.Body).Decode(&distanceMatrixResponse); err != nil {
		return nil, err
	}

	return &distanceMatrixResponse, nil
}
