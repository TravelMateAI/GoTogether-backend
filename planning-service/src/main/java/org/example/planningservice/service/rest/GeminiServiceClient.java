package org.example.planningservice.service.rest;

import org.example.planningservice.dto.GeminiHttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GeminiServiceClient {

    @Value("${gemini.service.url}")
    private String geminiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiHttpResponse getAIResponse(String prompt) {
        String url = UriComponentsBuilder
                .fromUriString(geminiServiceUrl)
                .queryParam("prompt", prompt)
                .toUriString();

        return restTemplate.getForObject(url, GeminiHttpResponse.class);
    }
}

