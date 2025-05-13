package com.example.socialmediaservice.dto;

import lombok.Data;

@Data
public class RequestCommentDTO {
    private String userId;
    private String content;
}