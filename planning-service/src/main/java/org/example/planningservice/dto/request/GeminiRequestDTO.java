package org.example.planningservice.dto.request;

public class GeminiRequestDTO {
    private String prompt;

    public GeminiRequestDTO() {
    }

    public GeminiRequestDTO(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
