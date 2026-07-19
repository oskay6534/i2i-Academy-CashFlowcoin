package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.market.dto.MarketPriceResponse;

import java.util.List;

public interface MarketPriceSearchIndexer {
    void index(List<MarketPriceResponse> prices);
}
