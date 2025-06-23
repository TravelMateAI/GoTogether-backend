package com.example.socialmediaservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowerInfo {
    private int followers;
    private boolean isFollowedByUser;
}
