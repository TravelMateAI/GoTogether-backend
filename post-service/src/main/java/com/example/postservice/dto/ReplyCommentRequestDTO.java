package com.example.postservice.dto;


import lombok.Data;

@Data
public class ReplyCommentRequestDTO {
    private String userId;
    private String content;
}

