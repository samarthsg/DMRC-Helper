package com.dmrc.helper.controller;

import com.dmrc.helper.dto.*;
import com.dmrc.helper.exception.AuthException;
import com.dmrc.helper.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Register a new user with email or phone number.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/verify-email
     * Verify a user's email using the token sent to their inbox.
     * Body: { "token": "..." }
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            throw new AuthException("Verification token is required", HttpStatus.BAD_REQUEST);
        }
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    /**
     * POST /api/auth/verify-phone
     * Verify a user's phone number using the OTP sent via SMS.
     * Body: { "phone": "...", "otp": "..." }
     */
    @PostMapping("/verify-phone")
    public ResponseEntity<Map<String, String>> verifyPhone(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String otp = body.get("otp");
        if (phone == null || phone.isBlank()) {
            throw new AuthException("Phone number is required", HttpStatus.BAD_REQUEST);
        }
        if (otp == null || otp.isBlank()) {
            throw new AuthException("OTP is required", HttpStatus.BAD_REQUEST);
        }
        authService.verifyPhone(phone, otp);
        return ResponseEntity.ok(Map.of("message", "Phone number verified successfully"));
    }

    /**
     * POST /api/auth/login
     * Login with email or phone number plus password.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/refresh-token
     * Refresh access token using a valid refresh token.
     * Body: { "refreshToken": "..." }
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException("Refresh token is required", HttpStatus.BAD_REQUEST);
        }
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/user-profile
     * Get the profile of the currently authenticated user (no sensitive data).
     */
    @GetMapping("/user-profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = authService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * Global exception handler for AuthException within this controller.
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("error", ex.getMessage()));
    }
}
