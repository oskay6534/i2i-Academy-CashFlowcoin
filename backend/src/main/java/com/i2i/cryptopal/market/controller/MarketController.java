package com.i2i.cryptopal.market.controller;

import com.i2i.cryptopal.market.service.MarketService;

import com.i2i.cryptopal.market.dto.MarketPriceResponse;
import com.i2i.cryptopal.market.dto.PriceHistoryResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/prices")
    public List<MarketPriceResponse> getLatestPrices() {
        return marketService.getLatestPrices();
    }

    @GetMapping("/history/{symbol}")
    public List<PriceHistoryResponse> getHistory(
        @PathVariable String symbol,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return marketService.getHistory(symbol, limit);
    }
}