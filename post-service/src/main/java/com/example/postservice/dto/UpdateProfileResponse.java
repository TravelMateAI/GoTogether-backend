package com.example.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateProfileResponse {
    private String userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
}
