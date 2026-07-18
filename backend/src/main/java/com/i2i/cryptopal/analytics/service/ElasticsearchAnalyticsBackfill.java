package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.market.service.MarketService;
import com.i2i.cryptopal.user.repository.AppUserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@ConditionalOnProperty(prefix = "app.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchAnalyticsBackfill implements ApplicationRunner {
    private final AppUserRepository userRepository;
    private final UserSearchIndexer userSearchIndexer;
    private final MarketService marketService;
    private final MarketPriceSearchIndexer marketPriceSearchIndexer;

    public ElasticsearchAnalyticsBackfill(
        AppUserRepository userRepository,
        UserSearchIndexer userSearchIndexer,
        MarketService marketService,
        MarketPriceSearchIndexer marketPriceSearchIndexer
    ) {
        this.userRepository = userRepository;
        this.userSearchIndexer = userSearchIndexer;
        this.marketService = marketService;
        this.marketPriceSearchIndexer = marketPriceSearchIndexer;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        userRepository.findAll().forEach(userSearchIndexer::index);
        marketPriceSearchIndexer.index(marketService.getLatestPrices());
    }
}
