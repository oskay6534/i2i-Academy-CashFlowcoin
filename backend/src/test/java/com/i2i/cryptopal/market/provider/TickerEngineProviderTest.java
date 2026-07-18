package com.i2i.cryptopal.market.provider;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TickerEngineProviderTest {

    private final TickerEngineProvider provider = new TickerEngineProvider();

    @Test
    void generatesPositivePricesForEveryProvidedSymbol() {
        Map<String, BigDecimal> currentPrices = Map.of(
            "BTC", new BigDecimal("65000.00000000"),
            "ETH", new BigDecimal("3500.00000000")
        );

        Map<String, BigDecimal> nextPrices =
            provider.generateNextPrices(currentPrices);

        assertThat(nextPrices).hasSameSizeAs(currentPrices);
        assertThat(nextPrices).containsOnlyKeys("BTC", "ETH");
        assertThat(nextPrices.values()).allSatisfy(price -> {
            assertThat(price).isGreaterThan(BigDecimal.ZERO);
            assertThat(price.scale()).isEqualTo(8);
        });
    }
}