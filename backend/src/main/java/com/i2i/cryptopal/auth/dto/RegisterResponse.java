package com.i2i.cryptopal.auth.dto;

import java.math.BigDecimal;

public record RegisterResponse(
    Long userId,
    String username,
    String email,
    BigDecimal initialBalance
) {
}
