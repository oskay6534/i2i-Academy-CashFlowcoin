package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.market.dto.MarketPriceResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.elasticsearch", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpMarketPriceSearchIndexer implements MarketPriceSearchIndexer {
    @Override
    public void index(List<MarketPriceResponse> prices) {
        // Elasticsearch is optional; Redis and PostgreSQL remain operational without it.
    }
}
