package com.i2i.cryptopal.portfolio.dto;

import java.math.BigDecimal;

public record PortfolioAssetResponse(
    String symbol,
    BigDecimal quantity,
    BigDecimal currentPrice,
    BigDecimal currentValue
) {
}