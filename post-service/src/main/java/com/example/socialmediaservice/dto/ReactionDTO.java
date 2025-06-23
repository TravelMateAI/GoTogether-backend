package com.example.socialmediaservice.dto;

import com.example.socialmediaservice.enums.ReactionType;
import lombok.Data;

@Data
public class ReactionDTO {
    private String userId;
    private ReactionType type;
}
