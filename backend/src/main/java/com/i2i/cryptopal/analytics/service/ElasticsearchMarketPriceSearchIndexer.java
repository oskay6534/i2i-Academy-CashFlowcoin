package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.market.dto.MarketPriceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchMarketPriceSearchIndexer implements MarketPriceSearchIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchMarketPriceSearchIndexer.class);
    private static final String INDEX_NAME = "cryptopal-market-prices";

    private final RestClient restClient;

    public ElasticsearchMarketPriceSearchIndexer(
        @Value("${app.elasticsearch.url}") String url,
        @Value("${app.elasticsearch.username}") String username,
        @Value("${app.elasticsearch.password}") String password
    ) {
        this.restClient = RestClient.builder().baseUrl(url)
            .defaultHeaders(headers -> headers.setBasicAuth(username, password)).build();
    }

    @Override
    public void index(List<MarketPriceResponse> prices) {
        prices.forEach(price -> {
            try {
                Map<String, Object> document = new LinkedHashMap<>();
                document.put("symbol", price.symbol());
                document.put("price", price.price());
                document.put("updatedAt", price.updatedAt().toString());
                restClient.put().uri("/{index}/_doc/{id}", INDEX_NAME, price.symbol())
                    .contentType(MediaType.APPLICATION_JSON).body(document).retrieve().toBodilessEntity();
            } catch (Exception exception) {
                LOGGER.warn("Market price for {} could not be indexed in Elasticsearch.", price.symbol(), exception);
            }
        });
    }
}
