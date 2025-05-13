package controllers

import (
	"api-service/services/gemini"
	"net/http"

	"github.com/gin-gonic/gin"
)

func LocationSummeryController(c *gin.Context) {
	location := c.Query("location")
	if location == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Missing location parameter"})
		return
	}

	result, err := gemini.CallGeminiAPI("assume the role of a travel guide and provide a summery on " + location + " in 100 words")
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to fetch location summary"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"summary": result.Text(), // Assuming the response has a 'Content' field for the summary
	})
}
