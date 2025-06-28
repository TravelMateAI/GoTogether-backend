package com.example.postservice.dto;

import com.example.postservice.enums.MediaType;
import com.example.postservice.enums.ReactionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PostDTO {
    private String postId;
    private String caption;
    private LocalDateTime createdAt;

    private UserDTO user;
    private List<MediaDTO> attachments;
    private List<CommentDTO> comments;
    private Map<ReactionType, Long> reactionCounts;
    private List<ReactionDTO> reactions;
    private List<UserIdDTO> bookmarks;

    private CountDTO _count;

    @Data
    public static class CountDTO {
        private int likes;
        private int comments;
    }
    @Data
    public static class UserIdDTO {
        private String userId;
    }

    @Data
    public static class ReactionDTO {
        private String userId;
        private ReactionType reactionType;
    }
}

