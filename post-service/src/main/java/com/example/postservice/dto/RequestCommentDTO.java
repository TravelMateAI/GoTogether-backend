package com.example.postservice.dto;

import lombok.Data;

@Data
public class RequestCommentDTO {
    private String userId;
    private String content;
}