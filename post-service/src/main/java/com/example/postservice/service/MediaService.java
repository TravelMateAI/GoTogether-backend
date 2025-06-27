package com.example.socialmediaservice.service;

import com.example.socialmediaservice.entity.Media;
import com.example.socialmediaservice.enums.MediaType;
import com.example.socialmediaservice.repository.MediaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepo mediaRepo;

    public Media createMedia(String url, MediaType type) {
        Media media = new Media();
        media.setUrl(url);
        media.setType(type);
        return mediaRepo.save(media);
    }

    public Optional<Media> getMediaById(String id) {
        return mediaRepo.findById(id);
    }
}
