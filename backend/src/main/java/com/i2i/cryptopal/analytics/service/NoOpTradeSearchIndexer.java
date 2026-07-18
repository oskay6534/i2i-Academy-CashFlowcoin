package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.trade.entity.TradeTransaction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.elasticsearch", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpTradeSearchIndexer implements TradeSearchIndexer {

    @Override
    public void index(TradeTransaction transaction) {
        // Elasticsearch is optional; PostgreSQL remains the source of truth.
    }
}