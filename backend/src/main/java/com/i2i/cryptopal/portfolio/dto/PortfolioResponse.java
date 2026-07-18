package com.i2i.cryptopal.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
    Long userId,
    BigDecimal cashBalance,
    BigDecimal cryptoValue,
    BigDecimal totalPortfolioValue,
    List<PortfolioAssetResponse> assets
) {
}