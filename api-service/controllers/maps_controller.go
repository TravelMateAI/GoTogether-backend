package controllers

import (
	"api-service/services/maps"
	"net/http"
	"strings" // Added strings import

	"github.com/gin-gonic/gin"
)

var directionsService = maps.NewDirectionsService()
var distanceMatrixService = maps.NewDistanceMatrixService()
var geocodeService = maps.NewGeocodeService()
var placesService = maps.NewPlacesService()

func DirectionsController(c *gin.Context) {
	// Logic to handle directions request
	origin := c.Query("origin")
	destination := c.Query("destination")

	directions, err := directionsService.GetDirections(origin, destination)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, directions)
}

func DistanceMatrixController(c *gin.Context) {
	// Logic to handle distance matrix request
	origins := c.Query("origins")
	destinations := c.Query("destinations")

	distanceMatrix, err := distanceMatrixService.GetDistanceMatrix(origins, destinations)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, distanceMatrix)
}

func GeocodeController(c *gin.Context) {
	address := c.Query("address")
	geocode, err := geocodeService.Geocode(address)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, geocode)
}

func LocationDataController(c *gin.Context) {
	query := c.Query("query")
	location := c.Query("location")
	places, err := placesService.SearchPlaces(query, location)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, places)
}

func PlaceDetailsController(c *gin.Context) {
	placeID := c.Param("place_id")
	if placeID == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "place_id parameter is required"})
		return
	}

	response, err := placesService.FindPlaceByID(placeID) // placesService is already initialized globally

	if err != nil {
		// Default to 500
		statusCode := http.StatusInternalServerError
		// Try to refine based on response content if available
		if response != nil {
			switch response.Status {
			case "NOT_FOUND":
				statusCode = http.StatusNotFound
			case "INVALID_REQUEST":
				statusCode = http.StatusBadRequest
			case "REQUEST_DENIED":
				statusCode = http.StatusForbidden // Or StatusBadRequest
			case "OVER_QUERY_LIMIT":
				statusCode = http.StatusTooManyRequests
			// No default case needed here, statusCode is already 500
			}
			c.JSON(statusCode, response) // Send the whole response from Google which includes error details
		} else {
			// If response is nil, just send the error message
			// Attempt to infer from error string for NOT_FOUND as a common case
			if strings.Contains(strings.ToUpper(err.Error()), "NOT_FOUND") {
				statusCode = http.StatusNotFound
			}
			c.JSON(statusCode, gin.H{"error": err.Error()})
		}
		return
	}

	// If err is nil, response must be non-nil and response.Status should be "OK"
	c.JSON(http.StatusOK, response)
}
