package com.example.Authservice.controller.dto;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class JwtAuthenticationResponseDto {
    @NonNull
    private String accessToken;
    private String refreshToken; // Optional, might not always be returned on access token refresh
    private String tokenType = "Bearer";

    public JwtAuthenticationResponseDto(@NonNull String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
