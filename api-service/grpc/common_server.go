package grpc

import (
	"context"
	"log"

	pb_common "api-service/grpc/pb/common" // Alias for the new common protobuf package
	// No more maps or gemini specific imports here
)

// ExampleGreeterServerImpl is used to implement pb_common.ExampleGreeterServer.
type ExampleGreeterServerImpl struct {
	pb_common.UnimplementedExampleGreeterServer
}

// SayHello implements pb_common.ExampleGreeterServer
func (s *ExampleGreeterServerImpl) SayHello(ctx context.Context, in *pb_common.HelloRequest) (*pb_common.HelloReply, error) {
	log.Printf("Received gRPC SayHello request: %v", in.GetName())
	return &pb_common.HelloReply{Message: "Hello " + in.GetName()}, nil
}

// Note: All MapsServerImpl, GeminiServerImpl, their specific service interfaces (IPlacesService, IAIService, etc.),
// and their patchable constructor functions (newPlacesServiceFunc, newAIServiceFunc, etc.)
// have been moved to maps_server.go and gemini_server.go respectively.
// This file now only contains common/example server implementations.
