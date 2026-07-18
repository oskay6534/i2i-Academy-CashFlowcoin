package com.i2i.cryptopal.trade.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TradeRequest(
    @NotBlank(message = "Cryptocurrency symbol is required")
    String symbol,

    @NotNull(message = "Quantity is required")
    @DecimalMin(
        value = "0.00000001",
        message = "Quantity must be greater than zero"
    )
    @Digits(
        integer = 18,
        fraction = 8,
        message = "Quantity can contain at most 8 decimal places"
    )
    BigDecimal quantity
) {
}