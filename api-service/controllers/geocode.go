package controllers

import (
	"api-service/services/maps"
	"net/http"

	"github.com/gin-gonic/gin"
)

func GeocodeController(c *gin.Context) {
	address := c.Query("address")
	if address == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Missing address parameter"})
		return
	}

	result, err := maps.CallGeocodeAPI(address)
	if err != nil || result.Status != "OK" || len(result.Results) == 0 {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch location"})
		return
	}

	res := result.Results[0]
	c.JSON(http.StatusOK, gin.H{
		"formatted_address": res.FormattedAddress,
		"latitude":          res.Geometry.Location.Lat,
		"longitude":         res.Geometry.Location.Lng,
	})
}
