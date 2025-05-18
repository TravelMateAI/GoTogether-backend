package controllers

import (
	"api-service/services/maps"
	"net/http"

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
