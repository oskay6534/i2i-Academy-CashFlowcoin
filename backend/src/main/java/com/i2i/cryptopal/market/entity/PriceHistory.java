package com.i2i.cryptopal.market.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "price_history",
    indexes = {
        @Index(
            name = "idx_price_history_symbol_recorded_at",
            columnList = "symbol, recorded_at"
        )
    }
)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public PriceHistory() {
    }

    public PriceHistory(
        String symbol,
        BigDecimal price,
        LocalDateTime recordedAt
    ) {
        this.symbol = symbol;
        this.price = price;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}