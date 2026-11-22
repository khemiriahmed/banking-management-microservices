package com.ahmedkh.banking.auth.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        List<String> roles
) {
}
