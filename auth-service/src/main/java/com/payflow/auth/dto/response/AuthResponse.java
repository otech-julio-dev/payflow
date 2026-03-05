package com.payflow.auth.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long   expiresIn,
    UserInfo user
) {
    public record UserInfo(Long id, String fullName, String email, String role) {}
}