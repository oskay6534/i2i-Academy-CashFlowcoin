package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.user.entity.AppUser;
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
public class ElasticsearchUserSearchIndexer implements UserSearchIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchUserSearchIndexer.class);
    private static final String INDEX_NAME = "cryptopal-users";

    private final RestClient restClient;

    public ElasticsearchUserSearchIndexer(
        @Value("${app.elasticsearch.url}") String url,
        @Value("${app.elasticsearch.username}") String username,
        @Value("${app.elasticsearch.password}") String password
    ) {
        this.restClient = RestClient.builder().baseUrl(url)
            .defaultHeaders(headers -> headers.setBasicAuth(username, password)).build();
    }

    @Override
    public void index(AppUser user) {
        try {
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("userId", user.getId());
            document.put("username", user.getUsername());
            document.put("email", user.getEmail());
            document.put("createdAt", user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString());
            restClient.put().uri("/{index}/_doc/{id}", INDEX_NAME, user.getId())
                .contentType(MediaType.APPLICATION_JSON).body(document).retrieve().toBodilessEntity();
        } catch (Exception exception) {
            LOGGER.warn("User {} could not be indexed in Elasticsearch; PostgreSQL user remains valid.", user.getId(), exception);
        }
    }
}
