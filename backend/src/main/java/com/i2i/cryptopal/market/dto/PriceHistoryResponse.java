package com.i2i.cryptopal.market.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryResponse(
    Long id,
    String symbol,
    BigDecimal price,
    LocalDateTime recordedAt
) {
}