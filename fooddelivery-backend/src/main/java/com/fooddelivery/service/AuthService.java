package com.fooddelivery.service;

import com.fooddelivery.dto.request.LoginRequest;
import com.fooddelivery.dto.request.RegisterRequest;
import com.fooddelivery.dto.response.AuthResponse;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.UserStatus;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fooddelivery.dto.request.VerifyOtpRequest;
import com.fooddelivery.dto.response.OtpResponseDto;
import com.fooddelivery.entity.Otp;
import com.fooddelivery.entity.OtpType;
import com.fooddelivery.repository.OtpRepository;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final PasswordValidationService passwordValidator;

    public OtpResponseDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            User existing = userRepository.findByEmail(request.getEmail()).get();
            if (existing.getStatus() == UserStatus.ACTIVE) {
                throw new RuntimeException("Email already in use");
            }
            // If exists but not active, we can resend OTP or update details
            existing.setName(request.getName());
            existing.setPhone(request.getPhone());
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            existing.setRole(request.getRole() != null ? request.getRole() : Role.ROLE_CUSTOMER);
            userRepository.save(existing);
        } else {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new RuntimeException("Phone number already in use");
            }
            passwordValidator.validatePassword(request.getPassword(), request.getName(), request.getEmail(), request.getPhone());
            User user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRole() != null ? request.getRole() : Role.ROLE_CUSTOMER)
                    .status(UserStatus.PENDING_VERIFICATION) // Wait for verification
                    .build();
            userRepository.save(user);
        }

        String otp = generateOtp();
        saveOtp(request.getEmail(), otp, OtpType.REGISTER);
        emailService.sendEmail(request.getEmail(), "Verify your email", "Your OTP is: " + otp);

        String authToken = tokenProvider.generateAuthToken(request.getEmail());

        return OtpResponseDto.builder()
                .message("OTP sent to email")
                .authToken(authToken)
                .email(request.getEmail())
                .build();
    }

    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        if (!tokenProvider.validateToken(request.getAuthToken())) {
            throw new RuntimeException("Invalid or expired auth token");
        }

        // Verify token subject matches email
        String tokenEmail = tokenProvider.getUsernameFromToken(request.getAuthToken());
        if (!tokenEmail.equals(request.getEmail())) {
            throw new RuntimeException("Token email mismatch");
        }

        Otp otpEntity = otpRepository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(request.getEmail(), request.getType())
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!otpEntity.getOtpCode().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP code");
        }

        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        if (request.getType() == OtpType.REGISTER) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }

        return loginWithoutPassword(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();

        return createAuthResponse(accessToken, refreshToken, user);
    }

    private AuthResponse loginWithoutPassword(User user) {
        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                user.getEmail(), "", java.util.Collections.emptyList());

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String accessToken = tokenProvider.generateToken(auth);
        String refreshToken = tokenProvider.generateRefreshToken(auth);
        return createAuthResponse(accessToken, refreshToken, user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }
        String email = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(email).orElseThrow();

        org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
                user.getEmail(), "", java.util.Collections.emptyList());

        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String newAccessToken = tokenProvider.generateToken(auth);
        String newRefreshToken = tokenProvider.generateRefreshToken(auth);

        return createAuthResponse(newAccessToken, newRefreshToken, user);
    }

    public void logout(String refreshToken) {
        // In a stateless JWT, we can't really "logout" without a blacklist.
        // For now, we will just return success as per spec.
    }

    private AuthResponse createAuthResponse(String accessToken, String refreshToken, User user) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(tokenProvider.getExpirationInSeconds());
        response.setUser(com.fooddelivery.dto.response.UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .build());
        return response;
    }

    private String generateOtp() {
        return String.valueOf(new java.util.Random().nextInt(900000) + 100000);
    }

    private void saveOtp(String email, String otpCode, OtpType type) {
        // Cleanup old OTPs
        java.util.List<Otp> oldOtps = otpRepository.findAllByEmailAndTypeAndUsedFalse(email, type);
        if (!oldOtps.isEmpty()) {
            otpRepository.deleteAll(oldOtps);
        }

        Otp otp = Otp.builder()
                .email(email)
                .otpCode(otpCode)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();
        otpRepository.save(otp);
    }

    public OtpResponseDto forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOtp();
        saveOtp(email, otp, OtpType.FORGOT_PASSWORD);
        emailService.sendEmail(email, "Reset Password", "Your OTP is: " + otp);

        String authToken = tokenProvider.generateAuthToken(email);

        return OtpResponseDto.builder()
                .message("OTP sent to email")
                .authToken(authToken)
                .email(email)
                .build();
    }
}
