package com.example.socialmediaservice.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String id;
    private String username;
    private String displayName;
    private String avatarUrl;
}
