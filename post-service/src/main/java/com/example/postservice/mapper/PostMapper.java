package com.example.postservice.mapper;

import com.example.postservice.dto.CreatePostResponseDTO;
import com.example.postservice.dto.MediaDTO;
import com.example.postservice.dto.PostDTO;
import com.example.postservice.dto.UserDTO;
import com.example.postservice.entity.Media;
import com.example.postservice.entity.Post;

import java.util.List;
import java.util.stream.Collectors;

public class PostMapper {
    public static CreatePostResponseDTO toDto(Post post) {
        CreatePostResponseDTO dto = new CreatePostResponseDTO();
        dto.setPostId(post.getPostId());
        dto.setCaption(post.getCaption()); // Updated from content → caption
        dto.setUserId(post.getUser().getUserId());
        dto.setUsername(post.getUser().getUsername());
        dto.setCreatedAt(post.getCreatedAt());

        // Convert media list
        dto.setMedia(post.getMediaList().stream()
                .map(PostMapper::toMediaDto)
                .collect(Collectors.toList()));

        return dto;
    }

    private static MediaDTO toMediaDto(Media media) {
        MediaDTO dto = new MediaDTO();
        dto.setId(media.getId());
        dto.setUrl(media.getUrl());
        dto.setType(media.getType());
        return dto;
    }

    public static PostDTO toDto2(Post post) {
        PostDTO dto = new PostDTO();
        dto.setPostId(post.getPostId());
        dto.setCaption(post.getCaption());
        dto.setCreatedAt(post.getCreatedAt());

        // ✅ Set nested user
        UserDTO userDTO = new UserDTO();
        userDTO.setId(post.getUser().getUserId());
        userDTO.setUsername(post.getUser().getUsername());
        userDTO.setDisplayName(post.getUser().getFirstName());
        userDTO.setAvatarUrl(post.getUser().getAvatarUrl());
        dto.setUser(userDTO);

        // ✅ Set attachments
        if (post.getMediaList() != null) {
            List<MediaDTO> attachments = post.getMediaList().stream()
                    .map(media -> {
                        MediaDTO mediaDTO = new MediaDTO();
                        mediaDTO.setId(media.getId());
                        mediaDTO.setUrl(media.getUrl());
                        mediaDTO.setType(media.getType());
                        return mediaDTO;
                    }).collect(Collectors.toList());
            dto.setAttachments(attachments);
        }

        return dto;
    }

    public static PostDTO toDTO(Post post) {
            PostDTO dto = new PostDTO();
            dto.setPostId(post.getPostId());
            dto.setCaption(post.getCaption());
            dto.setCreatedAt(post.getCreatedAt());

            UserDTO userDTO = new UserDTO();
            userDTO.setId(post.getUser().getUserId());
            userDTO.setUsername(post.getUser().getUsername());
            userDTO.setAvatarUrl(post.getUser().getAvatarUrl());
            dto.setUser(userDTO);

            // Optional: populate counts, reactions, attachments, etc. here

            return dto;
        }


}
