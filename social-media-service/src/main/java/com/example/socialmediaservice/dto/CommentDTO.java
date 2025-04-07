package com.example.socialmediaservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDTO {
    private String userId;
    private String content;
    private LocalDateTime createdAt;
    private String parentCommentId;
    private List<CommentDTO> replies; // nested replies
}
