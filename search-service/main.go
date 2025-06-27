package main

import (
	"api-service/config"
	grpchandler "api-service/grpc" // Alias for your gRPC server package

	// pb "api-service/grpc/pb"    // Old import - REMOVE
	pb_common "api-service/grpc/pb/common" // New
	pb_gemini "api-service/grpc/pb/gemini" // New
	pb_maps "api-service/grpc/pb/maps"     // New
	"api-service/routes"
	"log"
	"net"

	// "github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"google.golang.org/grpc"
)

func main() {
	config.LoadEnv()
	port := config.GetEnv("PORT", "8001")

	r := gin.Default()

	// // CORS Middleware Configuration
	// configCors := cors.DefaultConfig()
	// configCors.AllowAllOrigins = true
	// configCors.AllowMethods = []string{"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"}
	// configCors.AllowHeaders = []string{"Origin", "Content-Type", "Accept", "Authorization"}
	// configCors.ExposeHeaders = []string{"Content-Length"}
	// configCors.AllowCredentials = true

	// r.Use(cors.New(configCors))
	routes.RegisterRoutes(r)

	log.Printf("HTTP server listening on port %s", port)
	r.Run(":" + port)
}
