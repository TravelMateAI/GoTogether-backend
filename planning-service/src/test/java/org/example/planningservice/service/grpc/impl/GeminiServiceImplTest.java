package org.example.planningservice.service.grpc.impl;

import org.example.planningservice.grpc.apiservice.gemini.GeminiResponse; // Corrected import
import org.example.planningservice.grpc.apiservice.gemini.GeminiResponse.Candidate; // Corrected import
import org.example.planningservice.grpc.apiservice.gemini.GeminiResponse.Content; // Corrected import
import org.example.planningservice.grpc.apiservice.gemini.GeminiResponse.ContentPart; // Corrected import
import org.example.planningservice.grpc.client.GeminiClient; // Corrected import
import org.example.planningservice.dto.request.GeminiRequestDTO;
import org.example.planningservice.dto.response.GeminiResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GeminiServiceImplTest {

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private GeminiServiceImpl geminiService;

    private GeminiRequestDTO requestDTO;
    private GeminiResponse grpcResponse; // Corrected type
    private String prompt;
    private String expectedResponseText;

    @BeforeEach
    void setUp() {
        prompt = "Test prompt";
        requestDTO = new GeminiRequestDTO(prompt);

        expectedResponseText = "Generated content text";
        // Corrected builder according to local proto definition
        ContentPart part = ContentPart.newBuilder().setText(expectedResponseText).build();
        Content content = Content.newBuilder().addParts(part).build();
        Candidate candidate = Candidate.newBuilder().setContent(content).build();
        grpcResponse = GeminiResponse.newBuilder().addCandidates(candidate).build();
    }

    @Test
    void generateContent_shouldReturnMappedDTO_whenClientReturnsResponse() {
        // Arrange
        when(geminiClient.generateContent(prompt)).thenReturn(grpcResponse);

        // Act
        GeminiResponseDTO actualResponseDTO = geminiService.generateContent(requestDTO);

        // Assert
        assertEquals(expectedResponseText, actualResponseDTO.getGeneratedContent());
        verify(geminiClient).generateContent(prompt);
    }

    @Test
    void generateContent_shouldReturnEmptyDTO_whenClientReturnsEmptyResponse() {
        // Arrange
        GeminiResponse emptyGrpcResponse = GeminiResponse.newBuilder().build(); // Corrected type
        when(geminiClient.generateContent(prompt)).thenReturn(emptyGrpcResponse);

        // Act
        GeminiResponseDTO actualResponseDTO = geminiService.generateContent(requestDTO);

        // Assert
        assertEquals("", actualResponseDTO.getGeneratedContent());
        verify(geminiClient).generateContent(prompt);
    }

     @Test
    void generateContent_shouldReturnEmptyDTO_whenClientReturnsNullResponse() {
        // Arrange
        when(geminiClient.generateContent(prompt)).thenReturn(null);

        // Act
        GeminiResponseDTO actualResponseDTO = geminiService.generateContent(requestDTO);

        // Assert
        assertEquals("", actualResponseDTO.getGeneratedContent());
        verify(geminiClient).generateContent(prompt);
    }

    @Test
    void generateContent_shouldHandleNoCandidatesInResponse() {
        // Arrange
        GeminiResponse responseWithNoCandidates = GeminiResponse.newBuilder().clearCandidates().build(); // Corrected type
        when(geminiClient.generateContent(prompt)).thenReturn(responseWithNoCandidates);

        // Act
        GeminiResponseDTO actualResponseDTO = geminiService.generateContent(requestDTO);

        // Assert
        assertEquals("", actualResponseDTO.getGeneratedContent());
        verify(geminiClient).generateContent(prompt);
    }

    @Test
    void generateContent_shouldHandleNoPartsInCandidateContent() {
        // Arrange
        // Corrected builder according to local proto definition
        Content contentWithNoParts = Content.newBuilder().clearParts().build();
        Candidate candidateWithNoParts = Candidate.newBuilder().setContent(contentWithNoParts).build();
        GeminiResponse responseWithNoParts = GeminiResponse.newBuilder().addCandidates(candidateWithNoParts).build(); // Corrected type
        when(geminiClient.generateContent(prompt)).thenReturn(responseWithNoParts);

        // Act
        GeminiResponseDTO actualResponseDTO = geminiService.generateContent(requestDTO);

        // Assert
        assertEquals("", actualResponseDTO.getGeneratedContent());
        verify(geminiClient).generateContent(prompt);
    }

    @Test
    void generateContent_shouldHandleNoContentInCandidate() {
        // Arrange
        Candidate candidateWithNoContent = Candidate.newBuilder().clearContent().build();
        GeminiResponse responseWithNoContent = GeminiResponse.newBuilder().addCandidates(candidateWithNoContent).build();
        when(geminiClient.generateContent(prompt)).thenReturn(responseWithNoContent);

        // Act
        GeminiResponseDTO actualResponseDTO = geminiService.generateContent(requestDTO);

        // Assert
        assertEquals("", actualResponseDTO.getGeneratedContent());
        verify(geminiClient).generateContent(prompt);
    }
}
