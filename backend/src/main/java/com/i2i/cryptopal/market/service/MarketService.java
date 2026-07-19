package com.i2i.cryptopal.market.service;

import com.i2i.cryptopal.analytics.service.MarketPriceSearchIndexer;
import com.i2i.cryptopal.market.entity.PriceHistory;
import com.i2i.cryptopal.market.repository.PriceHistoryRepository;

import com.i2i.cryptopal.common.exception.InvalidMarketSymbolException;
import com.i2i.cryptopal.market.dto.MarketPriceResponse;
import com.i2i.cryptopal.market.dto.PriceHistoryResponse;
import com.i2i.cryptopal.market.provider.MarketDataProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketService {

    private static final String PRICE_PREFIX = "price:";
    private static final String VALUE_FIELD = "value";
    private static final String UPDATED_AT_FIELD = "updatedAt";

    private static final Map<String, BigDecimal> INITIAL_PRICES =
        Map.of(
            "BTC", new BigDecimal("65000.00000000"),
            "ETH", new BigDecimal("3500.00000000"),
            "SOL", new BigDecimal("145.00000000"),
            "BNB", new BigDecimal("580.00000000"),
            "XRP", new BigDecimal("0.55000000"),
            "ADA", new BigDecimal("0.45000000"),
            "DOGE", new BigDecimal("0.12000000"),
            "AVAX", new BigDecimal("32.00000000"),
            "DOT", new BigDecimal("6.50000000"),
            "LINK", new BigDecimal("14.00000000")
        );

    private final StringRedisTemplate redisTemplate;
    private final PriceHistoryRepository historyRepository;
    private final MarketDataProvider marketDataProvider;
    private final MarketPriceSearchIndexer marketPriceSearchIndexer;

    public MarketService(
        StringRedisTemplate redisTemplate,
        PriceHistoryRepository historyRepository,
        MarketDataProvider marketDataProvider,
        MarketPriceSearchIndexer marketPriceSearchIndexer
    ) {
        this.redisTemplate = redisTemplate;
        this.historyRepository = historyRepository;
        this.marketDataProvider = marketDataProvider;
        this.marketPriceSearchIndexer = marketPriceSearchIndexer;
    }

    @Scheduled(
        fixedRate = 15_000,
        initialDelay = 1_000
    )
    public void updateLatestPrices() {
        Map<String, BigDecimal> currentPrices = readCurrentPrices();

        if (currentPrices.isEmpty()) {
            currentPrices = new LinkedHashMap<>(INITIAL_PRICES);
        }

        Map<String, BigDecimal> nextPrices =
            marketDataProvider.generateNextPrices(currentPrices);

        Instant updatedAt = Instant.now();

        nextPrices.forEach((symbol, price) ->
            writePrice(symbol, price, updatedAt)
        );
        marketPriceSearchIndexer.index(getLatestPrices());
    }

    @Scheduled(
        fixedRate = 60_000,
        initialDelay = 10_000
    )
    @Transactional
    public void persistPriceSnapshots() {
        List<MarketPriceResponse> latestPrices = getLatestPrices();

        List<PriceHistory> snapshots = latestPrices.stream()
            .map(price -> new PriceHistory(
                price.symbol(),
                price.price(),
                LocalDateTime.ofInstant(
                    price.updatedAt(),
                    ZoneOffset.UTC
                )
            ))
            .toList();

        historyRepository.saveAll(snapshots);
    }

    public List<MarketPriceResponse> getLatestPrices() {
        ensurePricesExist();

        List<MarketPriceResponse> result = new ArrayList<>();

        INITIAL_PRICES.keySet()
            .stream()
            .sorted()
            .forEach(symbol -> {
                Map<Object, Object> values =
                    redisTemplate.opsForHash()
                        .entries(PRICE_PREFIX + symbol);

                String value = (String) values.get(VALUE_FIELD);
                String updatedAt = (String) values.get(
                    UPDATED_AT_FIELD
                );

                if (value != null && updatedAt != null) {
                    result.add(new MarketPriceResponse(
                        symbol,
                        new BigDecimal(value),
                        Instant.parse(updatedAt)
                    ));
                }
            });

        return result;
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getHistory(
        String rawSymbol,
        int requestedLimit
    ) {
        String symbol = normalizeAndValidateSymbol(rawSymbol);
        int limit = Math.max(1, Math.min(requestedLimit, 200));

        PageRequest pageRequest = PageRequest.of(
            0,
            limit,
            Sort.by(
                Sort.Direction.DESC,
                "recordedAt"
            )
        );

        return historyRepository
            .findBySymbol(symbol, pageRequest)
            .stream()
            .map(history -> new PriceHistoryResponse(
                history.getId(),
                history.getSymbol(),
                history.getPrice(),
                history.getRecordedAt()
            ))
            .toList();
    }

    private void ensurePricesExist() {
        boolean anyMissing = INITIAL_PRICES.keySet()
            .stream()
            .anyMatch(symbol ->
                !Boolean.TRUE.equals(
                    redisTemplate.hasKey(PRICE_PREFIX + symbol)
                )
            );

        if (!anyMissing) {
            return;
        }

        Instant now = Instant.now();

        INITIAL_PRICES.forEach((symbol, price) -> {
            if (!Boolean.TRUE.equals(
                redisTemplate.hasKey(PRICE_PREFIX + symbol)
            )) {
                writePrice(symbol, price, now);
            }
        });
    }

    private Map<String, BigDecimal> readCurrentPrices() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();

        INITIAL_PRICES.keySet().forEach(symbol -> {
            Object value = redisTemplate.opsForHash()
                .get(PRICE_PREFIX + symbol, VALUE_FIELD);

            if (value != null) {
                result.put(
                    symbol,
                    new BigDecimal(value.toString())
                );
            }
        });

        return result;
    }

    private void writePrice(
        String symbol,
        BigDecimal price,
        Instant updatedAt
    ) {
        String key = PRICE_PREFIX + symbol;

        redisTemplate.opsForHash().put(
            key,
            VALUE_FIELD,
            price.toPlainString()
        );

        redisTemplate.opsForHash().put(
            key,
            UPDATED_AT_FIELD,
            updatedAt.toString()
        );
    }

    private String normalizeAndValidateSymbol(String rawSymbol) {
        String symbol = rawSymbol
            .trim()
            .toUpperCase(Locale.ROOT);

        if (!INITIAL_PRICES.containsKey(symbol)) {
            throw new InvalidMarketSymbolException(
                "Unsupported cryptocurrency symbol: " + symbol
            );
        }

        return symbol;
    }
}
