package com.ISA.Restaurant.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Verification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String verificationCode;
    //    private String reason; // REGISTER, RESET_PASSWORD, etc.
    private LocalDateTime expiresAt;

    // Default (no-args) constructor required by JPA
    protected Verification() {
    }

    // Parameterized constructor for convenience
    public Verification(String email, String verificationCode, LocalDateTime expiresAt) {
        this.email = email;
        this.verificationCode = verificationCode;
        this.expiresAt = expiresAt;
    }

    // Getters and setters (optional but recommended)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
