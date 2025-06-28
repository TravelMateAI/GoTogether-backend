package com.example.postservice.controller;

import com.example.postservice.dto.MediaCreateRequestDTO;
import com.example.postservice.dto.MediaDTO;
import com.example.postservice.entity.Media;
import com.example.postservice.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping
    public ResponseEntity<MediaDTO> createMedia(@RequestBody MediaCreateRequestDTO requestDTO) {
        Media media = mediaService.createMedia(requestDTO.getUrl(), requestDTO.getType());
        MediaDTO dto = new MediaDTO();
        dto.setId(media.getId());
        dto.setUrl(media.getUrl());
        dto.setType(media.getType());
        return ResponseEntity.ok(dto);
    }
}