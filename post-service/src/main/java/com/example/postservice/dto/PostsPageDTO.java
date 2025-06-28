package com.example.postservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class PostsPageDTO {
    private List<PostDTO> posts;
    private String nextCursor;
}