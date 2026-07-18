package com.i2i.cryptopal.auth.dto;

import java.math.BigDecimal;

public record SessionResponse(
    Long userId,
    String username,
    String email,
    BigDecimal cashBalance
) {
}