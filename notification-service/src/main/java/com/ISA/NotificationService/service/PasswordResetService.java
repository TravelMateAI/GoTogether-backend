package com.ISA.Restaurant.service;

import com.ISA.Restaurant.Dto.Request.ForgetPasswordDto;
import com.ISA.Restaurant.Entity.Restaurant;
import com.ISA.Restaurant.exception.RestaurantNotFoundException;
import com.ISA.Restaurant.repo.RestaurantRepository;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PasswordResetService {

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    private final RestaurantRepository restaurantRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            RestaurantRepository restaurantRepository,
            JwtService jwtService,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.restaurantRepository = restaurantRepository;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Generate a reset token and send an email.
     */
    public String generateResetToken(ForgetPasswordDto dto) {
        log.info("Generating reset token for email: {}", dto.getEmail());

        Restaurant restaurant = restaurantRepository.findByRestaurantEmail(dto.getEmail())
                .orElseThrow(() -> new RestaurantNotFoundException("No restaurant found with email: " + dto.getEmail()));

        String resetToken = jwtService.generateResetToken(dto.getEmail());
        log.info("Generated reset token for email {}: {}", dto.getEmail(), resetToken);

        sendResetPasswordEmail(dto.getEmail(), resetToken);
        return resetToken;
    }

    /**
     * Update the password after token validation.
     */
    public void updatePassword(String resetToken, String newPassword) {
        String email = jwtService.validateResetToken(resetToken);

        Restaurant restaurant = restaurantRepository.findByRestaurantEmail(email)
                .orElseThrow(() -> new RuntimeException("No restaurant found with email: " + email));

        restaurant.setRestaurantPassword(passwordEncoder.encode(newPassword));
        restaurantRepository.save(restaurant);

        log.info("Password updated successfully for email: {}", email);
    }

    public String validateResetToken(String resetToken) {
        try {
            // Validate and extract the email from the token
            String email = jwtService.validateResetToken(resetToken);
            log.info("Reset token is valid for email: {}", email);

            // Optionally, check if the email exists in the database
            if (!restaurantRepository.findByRestaurantEmail(email).isPresent()) {
                throw new RestaurantNotFoundException("No restaurant found with email: " + email);
            }
            return email;
        } catch (Exception e) {
            log.error("Invalid or expired reset token: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired reset token");
        }
    }

    /**
     * Send a reset password email.
     */
    private void sendResetPasswordEmail(String email, String resetToken) {
        String subject = "Password Reset Request";
        String resetLink = baseUrl + ":5173/forgetpassword?token=" + resetToken;

        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif; background-color: #f9f9f9; margin: 0; padding: 0;\">"
                + "<table align=\"center\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width: 600px; background-color: #ffffff; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.1); margin: 20px auto; padding: 20px;\">"
                + "<tr>"
                + "<td align=\"center\" style=\"padding: 20px 0;\">"
                + "<h2 style=\"color: #333; margin: 0;\">Password Reset Request</h2>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td align=\"center\" style=\"padding: 10px 20px;\">"
                + "<p style=\"font-size: 16px; color: #666; margin: 0;\">You have requested to reset your password. Please click the link below to reset it:</p>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td align=\"center\" style=\"padding: 20px 0;\">"
                + "<a href=\"" + resetLink + "\" style=\"background-color: #007bff; color: #ffffff; padding: 15px 20px; border-radius: 8px; font-size: 16px; text-decoration: none;\">Reset Password</a>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td align=\"center\" style=\"padding: 20px 20px;\">"
                + "<p style=\"font-size: 14px; color: #999; margin: 0;\">If you did not request this, please ignore this email.</p>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "<footer style=\"text-align: center; font-size: 12px; color: #aaa; margin: 20px 0;\">"
                + "&copy; 2025 Our App, Inc. All rights reserved."
                + "</footer>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(email, subject, htmlMessage);
        } catch (MessagingException e) {
            log.error("Failed to send reset password email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Error sending email");
        }
    }
}
