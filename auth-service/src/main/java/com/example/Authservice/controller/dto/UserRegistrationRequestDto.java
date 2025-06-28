package com.example.Authservice.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationRequestDto {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(max = 80)
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 120)
    private String password;

    // Optional: Add other fields like firstName, lastName if desired for local registration
}
