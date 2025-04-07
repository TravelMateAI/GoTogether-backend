package com.example.socialmediaservice.dto;

import com.example.socialmediaservice.enums.ReactionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PostDTO {
    private String postId;
    private String content;
    private String userId;
    private String username;
    private LocalDateTime createdAt;
    private List<CommentDTO> comments;
    private Map<ReactionType, Long> reactionCounts;
}
