package grpc

import (
	"context"
	"errors"
	"os"
	"reflect"
	"testing"

	"api-service/config" // Required for GetEnv, though direct use is in server code
	"api-service/services/gemini" // For actual service, used by patchable func
	pb_gemini "api-service/grpc/pb/gemini"

	"github.com/google/generative-ai-go/genai"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// --- Mock Definition for Gemini ---
type MockAIService struct {
	GetAIResponseFunc func(prompt string) (interface{}, error)
}

func (m *MockAIService) GetAIResponse(prompt string) (interface{}, error) {
	if m.GetAIResponseFunc != nil {
		return m.GetAIResponseFunc(prompt)
	}
	return nil, errors.New("GetAIResponseFunc not implemented in mock")
}


// --- Test GenerateContent ---
func TestGeminiServerImpl_GenerateContent(t *testing.T) {
	server := &GeminiServerImpl{} // Uses the Impl from gemini_server.go
	ctx := context.Background()

	// Original newAIServiceFunc from gemini_server.go
	originalFunc := newAIServiceFunc
	defer func() { newAIServiceFunc = originalFunc }()

	mockService := &MockAIService{}
	// Patch the constructor function in gemini_server.go
	// The apiKey passed to the func will be from config.GetEnv in the actual server code
	newAIServiceFunc = func(apiKey string) IAIService { return mockService }

	// Store original API key and defer restoration
	originalAPIKey := os.Getenv("GOOGLE_GEMINI_API_KEY")
	defer func() {
		if err := os.Setenv("GOOGLE_GEMINI_API_KEY", originalAPIKey); err != nil {
			t.Logf("Warning: could not restore GOOGLE_GEMINI_API_KEY: %v", err)
		}
	}()


	tests := []struct {
		name         string
		req          *pb_gemini.GeminiRequest
		apiKeyEnv    string // Value to set for GOOGLE_GEMINI_API_KEY for this test
		mockSetup    func()
		expectedResp *pb_gemini.GeminiResponse
		expectedCode codes.Code
	}{
		{
			name:      "Success",
			req:       &pb_gemini.GeminiRequest{Prompt: "Hello Gemini"},
			apiKeyEnv: "test_api_key",
			mockSetup: func() {
				mockService.GetAIResponseFunc = func(prompt string) (interface{}, error) {
					return &genai.GenerateContentResponse{
						Candidates: []*genai.Candidate{
							{
								Index:        0,
								FinishReason: genai.FinishReasonStop,
								Content:      &genai.Content{Role: "model", Parts: []genai.Part{genai.Text("Hello there!")}},
								SafetyRatings: []*genai.SafetyRating{{Category: genai.HarmCategoryHarassment, Probability: genai.HarmProbabilityNegligible}},
							},
						},
						PromptFeedback: &genai.PromptFeedback{
							SafetyRatings: []*genai.SafetyRating{{Category: genai.HarmCategorySexuallyExplicit, Probability: genai.HarmProbabilityLow}},
						},
					}, nil
				}
			},
			expectedResp: &pb_gemini.GeminiResponse{
				Candidates: []*pb_gemini.GeminiResponse_Candidate{
					{
						Index:        0,
						FinishReason: genai.FinishReasonStop.String(),
						Content:      &pb_gemini.GeminiResponse_Content{Role: "model", Parts: []*pb_gemini.GeminiResponse_ContentPart{{Text: "Hello there!"}}},
						SafetyRatings: []*pb_gemini.GeminiResponse_SafetyRating{{Category: genai.HarmCategoryHarassment.String(), Probability: genai.HarmProbabilityNegligible.String()}},
					},
				},
				PromptFeedback: &pb_gemini.GeminiResponse_PromptFeedback{
					SafetyRatings: []*pb_gemini.GeminiResponse_SafetyRating{{Category: genai.HarmCategorySexuallyExplicit.String(), Probability: genai.HarmProbabilityLow.String()}},
				},
			},
			expectedCode: codes.OK,
		},
		{
			name:      "Service Error",
			req:       &pb_gemini.GeminiRequest{Prompt: "prompt_error"},
			apiKeyEnv: "test_api_key",
			mockSetup: func() {
				mockService.GetAIResponseFunc = func(prompt string) (interface{}, error) {
					return nil, errors.New("gemini service internal error")
				}
			},
			expectedResp: nil,
			expectedCode: codes.Internal,
		},
		{
			name:      "API Key Missing",
			req:       &pb_gemini.GeminiRequest{Prompt: "prompt_no_key"},
			apiKeyEnv: "", // API key is empty for this test
			mockSetup: func() {
				// No mock setup needed as it should fail before calling the service due to empty API key
			},
			expectedResp: nil,
			expectedCode: codes.FailedPrecondition,
		},
		{
			name:      "Invalid Response Type from Service",
			req:       &pb_gemini.GeminiRequest{Prompt: "prompt_bad_type"},
			apiKeyEnv: "test_api_key",
			mockSetup: func() {
				mockService.GetAIResponseFunc = func(prompt string) (interface{}, error) {
					return "not a *genai.GenerateContentResponse", nil // Return incorrect type
				}
			},
			expectedResp: nil,
			expectedCode: codes.Internal,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := os.Setenv("GOOGLE_GEMINI_API_KEY", tt.apiKeyEnv); err != nil {
				t.Fatalf("Failed to set GOOGLE_GEMINI_API_KEY for test: %v", err)
			}
			// Ensure config re-reads env var if it caches (not an issue with current config.GetEnv)
			// config.LoadEnv() // If config had a Load function that caches.

			tt.mockSetup() // Setup the mock function for this specific test case

			resp, err := server.GenerateContent(ctx, tt.req)

			if tt.expectedCode != codes.OK {
				if err == nil {
					t.Fatalf("Expected error code %v, got nil", tt.expectedCode)
				}
				st, ok := status.FromError(err)
				if !ok || st.Code() != tt.expectedCode {
					t.Fatalf("Expected gRPC status code %v, got %v (err: %v)", tt.expectedCode, st.Code(), err)
				}
			} else {
				if err != nil {
					t.Fatalf("Expected no error, got %v", err)
				}
				if !reflect.DeepEqual(resp, tt.expectedResp) {
					t.Errorf("Response mismatch:\nGot: %+v\nWant:%+v", resp, tt.expectedResp)
				}
			}
		})
	}
}
