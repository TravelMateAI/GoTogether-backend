package com.example.postservice.dto;

import com.example.postservice.enums.ReactionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CreatePostResponseDTO {
    private String postId;
    private String caption;
    private String userId;
    private String username;
    private LocalDateTime createdAt;
    private List<MediaDTO> media;
    private List<CommentDTO> comments;
    private Map<ReactionType, Long> reactionCounts;
}