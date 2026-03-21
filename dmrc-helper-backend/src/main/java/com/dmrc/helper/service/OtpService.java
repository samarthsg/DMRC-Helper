package com.dmrc.helper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a numeric OTP of the configured length.
     *
     * @return a 6-digit OTP string
     */
    public String generateOtp() {
        int otp = secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }

    /**
     * Sends an OTP to the given phone number via SMS.
     * In production, integrate with a real SMS gateway (e.g., Twilio, AWS SNS).
     * This is a mock implementation that logs the OTP.
     *
     * @param phoneNumber the recipient phone number
     * @param otp         the OTP to send
     */
    public void sendOtp(String phoneNumber, String otp) {
        log.info("[MOCK SMS] To: {} | OTP: {} | Message: Your DMRC Helper OTP is {}. Valid for 10 minutes.",
                phoneNumber, otp, otp);
    }
}
