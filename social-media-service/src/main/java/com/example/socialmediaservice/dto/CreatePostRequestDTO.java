package com.example.socialmediaservice.dto;

import lombok.Data;

@Data
public class CreatePostRequestDTO {
    private String content;
    private String email;
}
