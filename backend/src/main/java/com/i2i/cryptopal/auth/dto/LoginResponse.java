package com.i2i.cryptopal.auth.dto;

import java.math.BigDecimal;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresInSeconds,
    Long userId,
    String username,
    String email,
    BigDecimal cashBalance
) {
}