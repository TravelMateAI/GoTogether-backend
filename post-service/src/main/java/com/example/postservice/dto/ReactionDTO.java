package com.example.postservice.dto;

import com.example.postservice.enums.ReactionType;
import lombok.Data;

@Data
public class ReactionDTO {
    private String userId;
    private ReactionType type;
}
