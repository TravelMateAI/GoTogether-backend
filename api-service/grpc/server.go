package grpc

import (
	"context"
	"log"

	pb "api-service/proto_generated_go/proto" // Corrected import path
)

// server is used to implement proto.GreeterServer.
type Server struct {
	pb.UnimplementedGreeterServer
}

// SayHello implements proto.GreeterServer
func (s *Server) SayHello(ctx context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
	log.Printf("Received gRPC SayHello request: %v", in.GetName())
	return &pb.HelloReply{Message: "Hello " + in.GetName()}, nil
}
