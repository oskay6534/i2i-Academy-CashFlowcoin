package com.i2i.cryptopal.analytics.service;

import com.i2i.cryptopal.trade.entity.TradeTransaction;

public interface TradeSearchIndexer {

    void index(TradeTransaction transaction);
}