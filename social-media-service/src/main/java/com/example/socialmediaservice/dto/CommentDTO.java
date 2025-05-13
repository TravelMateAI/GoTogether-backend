package com.example.socialmediaservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentDTO {
    private String commentId;
    private UserDTO user;
    private String content;
    private LocalDateTime createdAt;
    private String parentCommentId;
    private List<CommentDTO> replies; // nested replies
}
