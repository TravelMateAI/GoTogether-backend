package com.ISA.Restaurant.service;

import com.ISA.Restaurant.Dto.Request.RequestVerificationDto;
import com.ISA.Restaurant.Dto.Request.VerifyEmailDto;
import com.ISA.Restaurant.Entity.Verification;
import com.ISA.Restaurant.repo.VerificationRepository;
import jakarta.mail.MessagingException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class VerificationService {
    private final VerificationRepository verificationRepository;
    private final EmailService emailService;

    public VerificationService(VerificationRepository verificationRepository, EmailService emailService) {
        this.verificationRepository = verificationRepository;
        this.emailService = emailService;
    }

    public void generateAndSendVerification(RequestVerificationDto requestVerificationDto) {
        String code = generateVerificationCode();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);

        // Check if a verification record already exists for the email
        Verification existingVerification = verificationRepository.findByEmail(requestVerificationDto.getEmail())
                .orElse(null);

        if (existingVerification != null) {
            // Update the existing record with a new code and expiration date
            existingVerification.setVerificationCode(code);
            existingVerification.setExpiresAt(expiryTime);
            verificationRepository.save(existingVerification);
            sendVerificationEmail(existingVerification);
        } else {
            // Create a new verification record
            Verification newVerification = new Verification(
                    requestVerificationDto.getEmail(),
                    code,
                    expiryTime
            );
            verificationRepository.save(newVerification);
            sendVerificationEmail(newVerification);
        }
    }



    public void verifyCode(VerifyEmailDto verifyEmailDto) {
        Verification verification = verificationRepository.findByEmailAndVerificationCode(
                        verifyEmailDto.getEmail(),
                        verifyEmailDto.getVerificationCode()
                )
                .orElseThrow(() -> new RuntimeException("Invalid or expired verification code"));
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code has expired");
        }
        verificationRepository.delete(verification);
    }

    public void resendVerificationCode(RequestVerificationDto requestVerificationDto) {
        Verification verification = verificationRepository.findByEmail(requestVerificationDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Verification not found"));
        generateAndSendVerification(requestVerificationDto);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }


    private void sendVerificationEmail(Verification verification) {
        String subject = "Account Verification";
        String verificationCode = verification.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif; background-color: #f9f9f9; margin: 0; padding: 0;\">"
                + "<table align=\"center\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width: 600px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); margin: 20px auto; padding: 20px;\">"
                + "<tr>"
                + "<td align=\"center\" style=\"padding: 20px 0;\">"
                + "<h2 style=\"color: #333; margin: 0;\">Welcome to Our App!</h2>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td align=\"center\" style=\"padding: 10px 20px;\">"
                + "<p style=\"font-size: 16px; color: #666; margin: 0;\">Please use the verification code below to continue:</p>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td align=\"center\" style=\"padding: 20px 0;\">"
                + "<div style=\"background-color: #007bff; color: #ffffff; padding: 15px 20px; border-radius: 8px; display: inline-block;\">"
                + "<span style=\"font-size: 24px; font-weight: bold;\">" + verificationCode + "</span>"
                + "</div>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td align=\"center\" style=\"padding: 20px 20px;\">"
                + "<p style=\"font-size: 14px; color: #999; margin: 0;\">This code will expire in 15 minutes. Please do not share it with anyone.</p>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td align=\"center\" style=\"padding: 20px 20px;\">"
                + "<a href=\"#\" style=\"font-size: 14px; color: #007bff; text-decoration: none;\">Need help? Contact Support</a>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "<footer style=\"text-align: center; font-size: 12px; color: #aaa; margin: 20px 0;\">"
                + "&copy; 2025 Our App, Inc. All rights reserved."
                + "</footer>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(verification.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void deleteExpiredVerifications() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(12);
        List<Verification> expiredVerifications = verificationRepository.findExpiredVerifications(threshold);

        if (!expiredVerifications.isEmpty()) {
            verificationRepository.deleteAll(expiredVerifications);
            System.out.println("Deleted " + expiredVerifications.size() + " expired verification records.");
        } else {
            System.out.println("No expired verification records to delete.");
        }
    }

}
