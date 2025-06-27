package com.example.socialmediaservice.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class UpdatePostRequestDTO {
    private String caption;
    private List<String> mediaIds; // Optional updates to media list
}