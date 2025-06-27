package com.ISA.Restaurant.Dto.Request;

import lombok.Data;

@Data
public class ResetPasswordDto {
    private String token;
    private String newPassword;
}
