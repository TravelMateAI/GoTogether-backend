package grpc

import (
	"context"
	"log"

	"api-service/config"
	"api-service/services/gemini"
	pb_gemini "api-service/grpc/pb/gemini" // Alias for the new gemini protobuf package

	"github.com/google/generative-ai-go/genai"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// --- Service Interface for Gemini ---
type IAIService interface {
	GetAIResponse(prompt string) (interface{}, error)
}

// --- Patchable Service Constructor for Gemini ---
var (
	newAIServiceFunc func(apiKey string) IAIService = func(apiKey string) IAIService { return gemini.NewAIService(apiKey) }
)

// GeminiServerImpl implements pb_gemini.GeminiServer
type GeminiServerImpl struct {
	pb_gemini.UnimplementedGeminiServer
}

// --- GeminiService Implementation ---
func (s *GeminiServerImpl) GenerateContent(ctx context.Context, req *pb_gemini.GeminiRequest) (*pb_gemini.GeminiResponse, error) {
	log.Printf("Received gRPC GenerateContent request for prompt (first 100 chars): %.100s", req.GetPrompt())

	apiKey := config.GetEnv("GOOGLE_GEMINI_API_KEY", "")
	if apiKey == "" {
		log.Printf("GOOGLE_GEMINI_API_KEY not set in environment")
		return nil, status.Errorf(codes.FailedPrecondition, "GOOGLE_GEMINI_API_KEY not configured")
	}

	aiService := newAIServiceFunc(apiKey)
	serviceResponseInterface, err := aiService.GetAIResponse(req.GetPrompt())

	if err != nil {
		log.Printf("Gemini AIService error: %v", err)
		return nil, status.Errorf(codes.Internal, "Gemini AIService failed: %v", err)
	}
	if serviceResponseInterface == nil {
		log.Printf("Gemini AIService returned nil interface")
		return nil, status.Errorf(codes.Internal, "Gemini AIService returned nil interface")
	}

	genaiResponse, ok := serviceResponseInterface.(*genai.GenerateContentResponse)
	if !ok {
		log.Printf("Gemini AIService response is not *genai.GenerateContentResponse: %T", serviceResponseInterface)
		return nil, status.Errorf(codes.Internal, "Gemini AIService response format error")
	}

	pbResp := &pb_gemini.GeminiResponse{}

	for _, candidate := range genaiResponse.Candidates {
		pbCandidate := &pb_gemini.GeminiResponse_Candidate{
			Index: int32(candidate.Index),
		}
		if candidate.FinishReason > 0 {
			pbCandidate.FinishReason = candidate.FinishReason.String()
		}

		if candidate.Content != nil {
			pbContent := &pb_gemini.GeminiResponse_Content{Role: candidate.Content.Role}
			for _, part := range candidate.Content.Parts {
				if textPart, ok := part.(genai.Text); ok {
					pbContent.Parts = append(pbContent.Parts, &pb_gemini.GeminiResponse_ContentPart{Text: string(textPart)})
				} else {
					log.Printf("Unhandled part type in candidate content: %T", part)
				}
			}
			pbCandidate.Content = pbContent
		}

		for _, safetyRating := range candidate.SafetyRatings {
			pbSafetyRating := &pb_gemini.GeminiResponse_SafetyRating{
				Category:    safetyRating.Category.String(),
				Probability: safetyRating.Probability.String(),
			}
			pbCandidate.SafetyRatings = append(pbCandidate.SafetyRatings, pbSafetyRating)
		}
		pbResp.Candidates = append(pbResp.Candidates, pbCandidate)
	}

	if genaiResponse.PromptFeedback != nil {
		pbPromptFeedback := &pb_gemini.GeminiResponse_PromptFeedback{}
		for _, safetyRating := range genaiResponse.PromptFeedback.SafetyRatings {
			pbSafetyRating := &pb_gemini.GeminiResponse_SafetyRating{
				Category:    safetyRating.Category.String(),
				Probability: safetyRating.Probability.String(),
			}
			pbPromptFeedback.SafetyRatings = append(pbPromptFeedback.SafetyRatings, pbSafetyRating)
		}
		pbResp.PromptFeedback = pbPromptFeedback
	}

	return pbResp, nil
}
