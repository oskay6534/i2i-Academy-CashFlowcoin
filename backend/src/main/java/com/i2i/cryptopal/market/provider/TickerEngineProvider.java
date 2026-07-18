package com.i2i.cryptopal.market.provider;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TickerEngineProvider implements MarketDataProvider {

    private static final BigDecimal MAXIMUM_CHANGE_PERCENT =
        new BigDecimal("0.015");

    private final SecureRandom random = new SecureRandom();

    @Override
    public Map<String, BigDecimal> generateNextPrices(
        Map<String, BigDecimal> currentPrices
    ) {
        Map<String, BigDecimal> nextPrices = new LinkedHashMap<>();

        currentPrices.forEach((symbol, currentPrice) -> {
            double randomUnit = (random.nextDouble() * 2.0) - 1.0;

            BigDecimal changeRate = MAXIMUM_CHANGE_PERCENT
                .multiply(BigDecimal.valueOf(randomUnit));

            BigDecimal nextPrice = currentPrice
                .multiply(BigDecimal.ONE.add(changeRate))
                .max(new BigDecimal("0.01"))
                .setScale(8, RoundingMode.HALF_UP);

            nextPrices.put(symbol, nextPrice);
        });

        return nextPrices;
    }
}