package grpc

import (
	"context"
	"testing"

	pb_common "api-service/grpc/pb/common" // Updated import
)

func TestSayHello(t *testing.T) {
	s := ExampleGreeterServerImpl{} // Use specific server implementation

	tests := []struct {
		name    string
		reqName string
		wantMsg string
	}{
		{
			name:    "StandardGreeting",
			reqName: "Jules",
			wantMsg: "Hello Jules",
		},
		{
			name:    "EmptyName",
			reqName: "",
			wantMsg: "Hello ",
		},
		{
			name:    "DifferentName",
			reqName: "Tester",
			wantMsg: "Hello Tester",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := &pb_common.HelloRequest{Name: tt.reqName} // Updated type
			resp, err := s.SayHello(context.Background(), req)
			if err != nil {
				t.Fatalf("SayHello(%v) got unexpected error: %v", req, err)
			}
			if resp.Message != tt.wantMsg {
				t.Errorf("SayHello(%v) = %q, want %q", req, resp.Message, tt.wantMsg)
			}
		})
	}
}
