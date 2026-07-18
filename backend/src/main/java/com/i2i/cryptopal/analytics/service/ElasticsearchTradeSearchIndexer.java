package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.trade.entity.TradeTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchTradeSearchIndexer implements TradeSearchIndexer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        ElasticsearchTradeSearchIndexer.class
    );
    private static final String INDEX_NAME = "cryptopal-trades";

    private final RestClient restClient;

    public ElasticsearchTradeSearchIndexer(
        @Value("${app.elasticsearch.url}") String url,
        @Value("${app.elasticsearch.username}") String username,
        @Value("${app.elasticsearch.password}") String password
    ) {
        this.restClient = RestClient.builder()
            .baseUrl(url)
            .defaultHeaders(headers -> headers.setBasicAuth(username, password))
            .build();
    }

    @Override
    public void index(TradeTransaction transaction) {
        try {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("transactionId", transaction.getId());
            document.put("userId", transaction.getUser().getId());
            document.put("username", transaction.getUser().getUsername());
            document.put("type", transaction.getType().name());
            document.put("symbol", transaction.getSymbol());
            document.put("quantity", transaction.getQuantity());
            document.put("executionPrice", transaction.getExecutionPrice());
            document.put("totalAmount", transaction.getTotalAmount());
            document.put("createdAt", transaction.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString());

            restClient.put()
                .uri("/{index}/_doc/{id}", INDEX_NAME, transaction.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(document)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception exception) {
            LOGGER.warn(
                "Trade {} could not be indexed in Elasticsearch; PostgreSQL transaction remains valid.",
                transaction.getId(),
                exception
            );
        }
    }
}