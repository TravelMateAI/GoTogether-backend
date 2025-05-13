package maps

import (
	"api-service/config"
	"api-service/models"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
)

func CallGeocodeAPI(address string) (*models.GeocodeResponse, error) {
	baseURL := "https://maps.googleapis.com/maps/api/geocode/json"
	query := url.Values{}
	query.Set("address", address)
	query.Set("key", config.GetEnv("GOOGLE_MAPS_API_KEY", ""))

	fullURL := fmt.Sprintf("%s?%s", baseURL, query.Encode())
	resp, err := http.Get(fullURL)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close() //after the end of the function, close the response body

	var geocode models.GeocodeResponse
	err = json.NewDecoder(resp.Body).Decode(&geocode)
	return &geocode, err
}
