package com.i2i.cryptopal.market.provider;

import java.math.BigDecimal;
import java.util.Map;

public interface MarketDataProvider {

    Map<String, BigDecimal> generateNextPrices(
        Map<String, BigDecimal> currentPrices
    );
}