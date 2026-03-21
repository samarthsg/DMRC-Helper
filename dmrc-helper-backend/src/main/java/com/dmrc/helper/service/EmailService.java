package com.dmrc.helper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    /**
     * Sends an email verification link to the user.
     * In a production environment, integrate with an SMTP provider (e.g., SendGrid, SES).
     * This is a mock implementation that logs the email details.
     *
     * @param toEmail   the recipient email address
     * @param token     the verification token
     */
    public void sendVerificationEmail(String toEmail, String token) {
        String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + token;
        log.info("[MOCK EMAIL] To: {} | Subject: Verify your DMRC Helper account | Link: {}",
                toEmail, verificationLink);
    }

    /**
     * Sends a password reset email.
     *
     * @param toEmail the recipient email address
     * @param token   the reset token
     */
    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("[MOCK EMAIL] To: {} | Subject: Reset your DMRC Helper password | Token: {}",
                toEmail, token);
    }
}
