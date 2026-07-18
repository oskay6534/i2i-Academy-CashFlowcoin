package com.i2i.cryptopal.trade.dto;

import com.i2i.cryptopal.trade.model.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    TradeType type,
    String symbol,
    BigDecimal quantity,
    BigDecimal executionPrice,
    BigDecimal totalAmount,
    LocalDateTime createdAt
) {
}