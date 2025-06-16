package org.example.planningservice.dto.response;

public class GeminiResponseDTO {
    private String generatedContent;

    public GeminiResponseDTO() {
    }

    public GeminiResponseDTO(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }
}
