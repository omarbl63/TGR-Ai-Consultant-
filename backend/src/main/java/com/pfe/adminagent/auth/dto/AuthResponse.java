package com.pfe.adminagent.auth.dto;

import com.pfe.adminagent.user.dto.UserDto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserDto user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserDto user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
