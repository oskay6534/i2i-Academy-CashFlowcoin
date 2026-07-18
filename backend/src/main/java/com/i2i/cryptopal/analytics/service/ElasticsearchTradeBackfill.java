package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.trade.repository.TradeTransactionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchTradeBackfill implements ApplicationRunner {

    private final TradeTransactionRepository transactionRepository;
    private final TradeSearchIndexer tradeSearchIndexer;

    public ElasticsearchTradeBackfill(
        TradeTransactionRepository transactionRepository,
        TradeSearchIndexer tradeSearchIndexer
    ) {
        this.transactionRepository = transactionRepository;
        this.tradeSearchIndexer = tradeSearchIndexer;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        transactionRepository.findAll().forEach(tradeSearchIndexer::index);
    }
}