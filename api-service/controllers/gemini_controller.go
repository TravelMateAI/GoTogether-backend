package controllers

import (
	"api-service/services/gemini"
	"net/http"
	"os"

	"github.com/gin-gonic/gin"
)

var AIService = gemini.NewAIService(os.Getenv("GOOGLE_GEMINI_API_KEY"))

func GeminiController(c *gin.Context) {
	prompt := c.Query("prompt")

	directions, err := AIService.GetAIResponse(prompt)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	c.JSON(http.StatusOK, directions)
}
