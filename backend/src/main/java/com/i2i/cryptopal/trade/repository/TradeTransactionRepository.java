package com.i2i.cryptopal.trade.repository;

import com.i2i.cryptopal.trade.entity.TradeTransaction;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeTransactionRepository
    extends JpaRepository<TradeTransaction, Long> {

    List<TradeTransaction> findByUserId(
        Long userId,
        Pageable pageable
    );
}