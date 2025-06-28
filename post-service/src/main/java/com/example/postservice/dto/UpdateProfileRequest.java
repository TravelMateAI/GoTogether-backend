package com.example.postservice.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String displayName;
    private String bio;
    private String avatarUrl; // optional
}
