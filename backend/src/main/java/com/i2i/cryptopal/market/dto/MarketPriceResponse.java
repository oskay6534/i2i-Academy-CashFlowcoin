package com.i2i.cryptopal.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketPriceResponse(
    String symbol,
    BigDecimal price,
    Instant updatedAt
) {
}