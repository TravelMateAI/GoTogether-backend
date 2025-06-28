package com.example.postservice.dto;

import com.example.postservice.enums.MediaType;
import lombok.Data;

@Data
public class MediaCreateRequestDTO {
    private String url;
    private MediaType type; // IMAGE or VIDEO
}
