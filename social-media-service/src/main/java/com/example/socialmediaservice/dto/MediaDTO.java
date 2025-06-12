package com.example.socialmediaservice.dto;

import com.example.socialmediaservice.enums.MediaType;
import lombok.Data;

@Data
public class MediaDTO {
    private String id;
    private String url;
    private MediaType type; // IMAGE or VIDEO
}
