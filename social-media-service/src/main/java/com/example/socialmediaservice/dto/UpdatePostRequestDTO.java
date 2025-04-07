package com.example.socialmediaservice.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdatePostRequestDTO {
    private String content;
}
