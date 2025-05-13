package main

import (
	"api-service/config"
	"api-service/routes"

	"github.com/gin-gonic/gin"
)

func main() {
	config.LoadEnv()
	port := config.GetEnv("PORT", "8080")

	r := gin.Default()
	routes.RegisterRoutes(r)

	r.Run(":" + port)
}
