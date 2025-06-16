package org.example.planningservice.service.rest;

import org.example.planningservice.dto.UserTripHistoryDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${social-media-service.url}")
    private String socialMediaServiceUrl;

    @Value("${social-media-service.trip-history-path}")
    private String socialMediaServiceTripHistoryPath;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserTripHistoryDTO getUserTripHistory(String userId) {
        String url = socialMediaServiceUrl + socialMediaServiceTripHistoryPath + userId;
        return restTemplate.getForObject(url, UserTripHistoryDTO.class);
    }
}

