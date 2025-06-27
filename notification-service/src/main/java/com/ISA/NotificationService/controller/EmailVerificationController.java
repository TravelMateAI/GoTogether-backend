package com.ISA.Restaurant.controller;

import com.ISA.Restaurant.Dto.Request.RequestVerificationDto;
import com.ISA.Restaurant.Dto.Request.VerifyEmailDto;
import com.ISA.Restaurant.service.VerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/email")
@RestController
public class EmailVerificationController {
    private final VerificationService verificationService;

    public EmailVerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    // Test endpoint for checking the API
    @GetMapping("/test")
    public String test() {
        log.info("hit test");
        return "test";
    }

    @PostMapping("/request")
    public ResponseEntity<?> requestVerificationCode(@RequestBody RequestVerificationDto requestVerificationDto) {
        try {
            verificationService.generateAndSendVerification(requestVerificationDto);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyEmailDto verifyEmailDto) {
        try {
            verificationService.verifyCode(verifyEmailDto);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestBody RequestVerificationDto requestVerificationDto) {
        try {
            verificationService.resendVerificationCode(requestVerificationDto);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
