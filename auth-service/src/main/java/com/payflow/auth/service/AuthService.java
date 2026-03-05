package com.payflow.auth.service;

import com.payflow.auth.dto.request.*;
import com.payflow.auth.dto.response.AuthResponse;
import com.payflow.auth.entity.User;
import com.payflow.auth.exception.EmailAlreadyExistsException;
import com.payflow.auth.repository.UserRepository;
import com.payflow.auth.security.JwtService;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authManager) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
        this.authManager     = authManager;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException(req.email());
        }

        User user = new User(
            req.fullName(),
            req.email(),
            passwordEncoder.encode(req.password())
        );
        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest req) {
        if (!jwtService.isValid(req.refreshToken())) {
            throw new BadCredentialsException("Refresh token inválido o expirado");
        }
        String email = jwtService.extractEmail(req.refreshToken());
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));
        return buildAuthResponse(user);
    }

    // ── Private ───────────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user) {
        Map<String, Object> claims = Map.of(
            "role",   user.getRole().name(),
            "userId", user.getId()
        );
        String accessToken  = jwtService.generateAccessToken(user.getEmail(), claims);
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(
            accessToken,
            refreshToken,
            "Bearer",
            jwtService.getExpirationMs() / 1000,
            new AuthResponse.UserInfo(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
            )
        );
    }
}