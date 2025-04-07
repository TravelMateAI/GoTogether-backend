package com.example.socialmediaservice.mapper;


import com.example.socialmediaservice.dto.PostDTO;
import com.example.socialmediaservice.entity.Post;

public class PostMapper {
    public static PostDTO toDto(Post post) {
        PostDTO dto = new PostDTO();
        dto.setPostId(post.getPostId());
        dto.setContent(post.getContent());
        dto.setUserId(post.getUser().getUserId());
        dto.setUsername(post.getUser().getUsername());
        dto.setCreatedAt(post.getCreatedAt());
        return dto;
    }
}
