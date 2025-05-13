package maps

import (
	"api-service/config"
	"api-service/models"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
)

func CallPlacesAPI(location string, radius int, placeType string) (*models.PlacesResponse, error) {
	baseURL := "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
	query := url.Values{}
	query.Set("location", location)
	query.Set("radius", fmt.Sprintf("%d", radius))
	query.Set("type", placeType)
	query.Set("key", config.GetEnv("GOOGLE_MAPS_API_KEY", ""))

	fullURL := fmt.Sprintf("%s?%s", baseURL, query.Encode())
	resp, err := http.Get(fullURL)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close() //after the end of the function, close the response body

	var place models.PlacesResponse
	err = json.NewDecoder(resp.Body).Decode(&place)
	return &place, err
}
