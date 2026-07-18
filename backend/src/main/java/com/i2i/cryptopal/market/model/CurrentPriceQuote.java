package com.i2i.cryptopal.market.model;

import java.math.BigDecimal;

public record CurrentPriceQuote(
    String symbol,
    BigDecimal price
) {
}