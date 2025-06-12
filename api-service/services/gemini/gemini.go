package gemini

import (
	"api-service/config"
	"context"
	"net/http"
	"time"

	"google.golang.org/genai"
)

type AIService struct {
	client *http.Client
	apiKey string
}

func NewAIService(apiKey string) *AIService {
	return &AIService{
		client: &http.Client{Timeout: 10 * time.Second},
		apiKey: apiKey,
	}
}

func (ais *AIService) GetAIResponse(prompt string) (interface{}, error) {
	ctx := context.Background()
	client, err := genai.NewClient(ctx, &genai.ClientConfig{
		APIKey:  config.GetEnv("GOOGLE_GEMINI_API_KEY", ""),
		Backend: genai.BackendGeminiAPI,
	})
	if err != nil {
		return nil, err
	}
	resp, err := client.Models.GenerateContent(
		ctx,
		"gemini-2.0-flash",
		genai.Text(prompt),
		nil,
	)
	if err != nil {
		return nil, err
	}

	return resp, nil
}
