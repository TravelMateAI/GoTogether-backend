package gemini

import (
	"context"

	"api-service/config"

	"google.golang.org/genai"
)

func CallGeminiAPI(prompt string) (*genai.GenerateContentResponse, error) {
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
