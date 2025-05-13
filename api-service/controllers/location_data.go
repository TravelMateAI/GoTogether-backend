package controllers

import (
	"api-service/models"
	"api-service/services/maps"
	"net/http"
	"strconv"
	"sync"

	"github.com/gin-gonic/gin"
)

func LocationDataController(c *gin.Context) {
	location := c.Query("location")
	radius, r_err := strconv.Atoi(c.Query("radius"))
	placeTypes := c.QueryArray("types")

	if location == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Missing location parameter"})
		return
	}
	if r_err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Error in radius parameter"})
		return
	}
	if len(placeTypes) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Missing type parameter"})
		return
	}

	var wg sync.WaitGroup
	results := make(chan *models.PlacesResponse, len(placeTypes))
	errors := make(chan error, len(placeTypes))

	for _, placeType := range placeTypes {
		wg.Add(1)
		go func(placeType string) {
			defer wg.Done()
			result, err := maps.CallPlacesAPI(location, radius, placeType)
			if err != nil || result.Status != "OK" || len(result.Results) == 0 {
				errors <- err
				return
			}
			results <- result
		}(placeType)
	}

	wg.Wait()
	close(results)
	close(errors)

	// Check if there were any errors
	if len(errors) > 0 {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch location data"})
		return
	}

	// Aggregate results
	var aggregatedResults []models.PlacesResponse
	for result := range results {
		aggregatedResults = append(aggregatedResults, *result)
	}

	c.JSON(http.StatusOK, gin.H{
		"status":  "OK",
		"results": aggregatedResults,
	})
}
