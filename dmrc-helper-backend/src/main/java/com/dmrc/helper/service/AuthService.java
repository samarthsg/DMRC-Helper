package com.dmrc.helper.service;

import com.dmrc.helper.dto.*;
import com.dmrc.helper.entity.User;
import com.dmrc.helper.entity.VerificationToken;
import com.dmrc.helper.exception.AuthException;
import com.dmrc.helper.repository.UserRepository;
import com.dmrc.helper.repository.VerificationTokenRepository;
import com.dmrc.helper.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9]{10,15}$");

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final OtpService otpService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        User user;
        boolean hasValidEmail = request.getEmail() != null
                && !request.getEmail().isBlank()
                && EMAIL_PATTERN.matcher(request.getEmail().trim()).matches();

        if (hasValidEmail) {
            user = registerWithEmail(request);
        } else {
            user = registerWithPhone(request);
        }

        String loginId = user.getRegistrationType() == User.RegistrationType.EMAIL
                ? user.getEmail() : user.getPhone();

        String accessToken = jwtTokenProvider.generateAccessToken(loginId);
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .userId(user.getUserId())
                .registrationType(user.getRegistrationType())
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository
                .findByTokenAndTokenType(token, VerificationToken.TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new AuthException("Invalid verification token", HttpStatus.BAD_REQUEST));

        if (verificationToken.isUsed()) {
            throw new AuthException("Verification token already used", HttpStatus.BAD_REQUEST);
        }
        if (verificationToken.isExpired()) {
            throw new AuthException("Verification token has expired", HttpStatus.BAD_REQUEST);
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
    }

    @Transactional
    public void verifyPhone(String phone, String otp) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        VerificationToken verificationToken = tokenRepository
                .findByTokenAndTokenType(otp, VerificationToken.TokenType.PHONE_OTP)
                .orElseThrow(() -> new AuthException("Invalid OTP", HttpStatus.BAD_REQUEST));

        if (!verificationToken.getUser().getUserId().equals(user.getUserId())) {
            throw new AuthException("Invalid OTP for this phone number", HttpStatus.BAD_REQUEST);
        }
        if (verificationToken.isUsed()) {
            throw new AuthException("OTP already used", HttpStatus.BAD_REQUEST);
        }
        if (verificationToken.isExpired()) {
            throw new AuthException("OTP has expired", HttpStatus.BAD_REQUEST);
        }

        user.setPhoneVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLoginId(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (BadCredentialsException e) {
            throw new AuthException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(request.getLoginId())
                .or(() -> userRepository.findByPhone(request.getLoginId()))
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        String accessToken = jwtTokenProvider.generateAccessToken(request.getLoginId());
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .userId(user.getUserId())
                .registrationType(user.getRegistrationType())
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        VerificationToken storedToken = tokenRepository
                .findByTokenAndTokenType(refreshToken, VerificationToken.TokenType.REFRESH_TOKEN)
                .orElseThrow(() -> new AuthException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (storedToken.isUsed()) {
            throw new AuthException("Refresh token already used", HttpStatus.UNAUTHORIZED);
        }
        if (storedToken.isExpired()) {
            throw new AuthException("Refresh token has expired. Please login again.", HttpStatus.UNAUTHORIZED);
        }

        User user = storedToken.getUser();
        storedToken.setUsed(true);
        tokenRepository.save(storedToken);

        String loginId = user.getRegistrationType() == User.RegistrationType.EMAIL
                ? user.getEmail() : user.getPhone();

        String newAccessToken = jwtTokenProvider.generateAccessToken(loginId);
        String newRefreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs() / 1000)
                .userId(user.getUserId())
                .registrationType(user.getRegistrationType())
                .build();
    }

    public UserProfileResponse getUserProfile(String loginId) {
        User user = userRepository.findByEmail(loginId)
                .or(() -> userRepository.findByPhone(loginId))
                .orElseThrow(() -> new AuthException("User not found", HttpStatus.NOT_FOUND));

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .isEmailVerified(user.isEmailVerified())
                .isPhoneVerified(user.isPhoneVerified())
                .registrationType(user.getRegistrationType())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private User registerWithEmail(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new AuthException("Email already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .registrationType(User.RegistrationType.EMAIL)
                .build();
        user = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        tokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(email, token);
        return user;
    }

    private User registerWithPhone(RegisterRequest request) {
        String phone = normalizePhone(request.getPhone());
        if (userRepository.existsByPhone(phone)) {
            throw new AuthException("Phone number already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .registrationType(User.RegistrationType.PHONE)
                .build();
        user = userRepository.save(user);

        String otp = otpService.generateOtp();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(otp)
                .user(user)
                .tokenType(VerificationToken.TokenType.PHONE_OTP)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        tokenRepository.save(verificationToken);

        otpService.sendOtp(phone, otp);
        return user;
    }

    private String createRefreshToken(User user) {
        String loginId = user.getRegistrationType() == User.RegistrationType.EMAIL
                ? user.getEmail() : user.getPhone();
        String rawToken = jwtTokenProvider.generateRefreshToken(loginId);

        tokenRepository.findByUserAndTokenTypeAndUsedFalse(user, VerificationToken.TokenType.REFRESH_TOKEN)
                .ifPresent(existing -> {
                    existing.setUsed(true);
                    tokenRepository.save(existing);
                });

        VerificationToken refreshToken = VerificationToken.builder()
                .token(rawToken)
                .user(user)
                .tokenType(VerificationToken.TokenType.REFRESH_TOKEN)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .used(false)
                .build();
        tokenRepository.save(refreshToken);

        return rawToken;
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new AuthException("Password must be at least 8 characters", HttpStatus.BAD_REQUEST);
        }

        boolean hasEmail = request.getEmail() != null && !request.getEmail().isBlank();
        boolean hasPhone = request.getPhone() != null && !request.getPhone().isBlank();

        if (!hasEmail && !hasPhone) {
            throw new AuthException("Either email or phone number is required", HttpStatus.BAD_REQUEST);
        }

        if (hasEmail && !EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new AuthException("Invalid email format", HttpStatus.BAD_REQUEST);
        }

        if (hasPhone && !PHONE_PATTERN.matcher(request.getPhone().trim()).matches()) {
            throw new AuthException("Invalid phone number format. Use 10-15 digits.", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            throw new AuthException("Phone number is required", HttpStatus.BAD_REQUEST);
        }
        return phone.trim().replaceAll("\\s+", "");
    }
}
