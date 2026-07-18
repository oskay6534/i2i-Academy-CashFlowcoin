package com.i2i.cryptopal.market.service;

import com.i2i.cryptopal.market.model.CurrentPriceQuote;

import com.i2i.cryptopal.common.exception.InvalidMarketSymbolException;
import com.i2i.cryptopal.common.exception.InvalidTradeException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

@Service
public class CurrentPriceService {

    private static final String PRICE_PREFIX = "price:";
    private static final String VALUE_FIELD = "value";
    private static final Set<String> SUPPORTED_SYMBOLS =
        Set.of("BTC", "ETH", "SOL");

    private final StringRedisTemplate redisTemplate;

    public CurrentPriceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public CurrentPriceQuote getCurrentPrice(String rawSymbol) {
        String symbol = normalizeAndValidate(rawSymbol);

        Object value = redisTemplate.opsForHash()
            .get(PRICE_PREFIX + symbol, VALUE_FIELD);

        if (value == null) {
            throw new InvalidTradeException(
                "Current price is not available for " + symbol
            );
        }

        try {
            return new CurrentPriceQuote(
                symbol,
                new BigDecimal(value.toString())
            );
        } catch (NumberFormatException exception) {
            throw new InvalidTradeException(
                "Current price data is invalid for " + symbol
            );
        }
    }

    private String normalizeAndValidate(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            throw new InvalidMarketSymbolException(
                "Cryptocurrency symbol is required"
            );
        }

        String symbol = rawSymbol
            .trim()
            .toUpperCase(Locale.ROOT);

        if (!SUPPORTED_SYMBOLS.contains(symbol)) {
            throw new InvalidMarketSymbolException(
                "Unsupported cryptocurrency symbol: " + symbol
            );
        }

        return symbol;
    }
}