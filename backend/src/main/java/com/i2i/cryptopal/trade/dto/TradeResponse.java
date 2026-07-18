package com.i2i.cryptopal.trade.dto;

import com.i2i.cryptopal.trade.model.TradeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeResponse(
    Long transactionId,
    TradeType type,
    String symbol,
    BigDecimal quantity,
    BigDecimal executionPrice,
    BigDecimal totalAmount,
    BigDecimal cashBalance,
    BigDecimal assetQuantity,
    LocalDateTime executedAt
) {
}