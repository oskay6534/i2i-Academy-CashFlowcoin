package com.i2i.cryptopal.market.repository;

import com.i2i.cryptopal.market.entity.PriceHistory;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository
    extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findBySymbol(
        String symbol,
        Pageable pageable
    );
}