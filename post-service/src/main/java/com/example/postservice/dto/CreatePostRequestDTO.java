package com.example.postservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequestDTO {
    private String caption;
    private String email;
    private List<String> mediaIds; // Optional list of media IDs (UUID strings)
}
