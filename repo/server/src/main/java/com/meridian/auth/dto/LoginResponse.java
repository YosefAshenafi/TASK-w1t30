package com.meridian.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        int expiresIn,
        UserProfileDto user,
        boolean newDeviceAlertRaised
) {
    public static final int EXPIRES_IN = 900;
}
