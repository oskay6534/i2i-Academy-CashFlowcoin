package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.user.entity.AppUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(UserSearchIndexer.class)
public class NoOpUserSearchIndexer implements UserSearchIndexer {
    @Override
    public void index(AppUser user) {
        // Elasticsearch is optional; PostgreSQL remains the source of truth.
    }
}
