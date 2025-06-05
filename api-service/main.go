package main

import (
	"api-service/config"
	grpchandler "api-service/grpc" // Alias for your gRPC server package
	pb "api-service/proto_generated_go/proto" // Alias for your generated proto package
	"api-service/routes"
	"log"
	"net"

	"github.com/gin-gonic/gin"
	"google.golang.org/grpc"
)

func main() {
	config.LoadEnv()
	port := config.GetEnv("PORT", "8000")

	// Start gRPC server in a goroutine
	go func() {
		lis, err := net.Listen("tcp", ":50051") // gRPC server port
		if err != nil {
			log.Fatalf("failed to listen for gRPC: %v", err)
		}
		s := grpc.NewServer()
		pb.RegisterGreeterServer(s, &grpchandler.Server{}) // Assumes your server struct is named Server
		log.Printf("gRPC server listening at %v", lis.Addr())
		if err := s.Serve(lis); err != nil {
			log.Fatalf("failed to serve gRPC: %v", err)
		}
	}()

	r := gin.Default()
	routes.RegisterRoutes(r)

	log.Printf("HTTP server listening on port %s", port)
	r.Run(":" + port)
}
