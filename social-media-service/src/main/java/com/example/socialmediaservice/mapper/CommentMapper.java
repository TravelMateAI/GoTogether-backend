package com.example.socialmediaservice.mapper;


import com.example.socialmediaservice.dto.CommentDTO;
import com.example.socialmediaservice.entity.Comment;

import java.util.stream.Collectors;

public class CommentMapper {

    private CommentDTO toDtoWithReplies(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setUserId(comment.getUser().getUserId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setReplies(comment.getReplies().stream()
                .map(this::toDtoWithReplies)
                .collect(Collectors.toList()));
        return dto;
    }

}
