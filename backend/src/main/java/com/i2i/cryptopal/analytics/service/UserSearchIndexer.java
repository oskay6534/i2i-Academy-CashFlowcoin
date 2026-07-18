package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.user.entity.AppUser;

public interface UserSearchIndexer {
    void index(AppUser user);
}
