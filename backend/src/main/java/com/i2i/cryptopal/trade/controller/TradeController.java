package com.i2i.cryptopal.trade.controller;

import com.i2i.cryptopal.trade.service.TradeService;

import com.i2i.cryptopal.portfolio.dto.PortfolioResponse;
import com.i2i.cryptopal.trade.dto.TradeRequest;
import com.i2i.cryptopal.trade.dto.TradeResponse;
import com.i2i.cryptopal.trade.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping("/trades/buy")
    public TradeResponse buy(
        @RequestHeader(value = "Authorization", required = false)
        String authorizationHeader,
        @Valid @RequestBody TradeRequest request
    ) {
        return tradeService.buy(
            authorizationHeader,
            request
        );
    }

    @PostMapping("/trades/sell")
    public TradeResponse sell(
        @RequestHeader(value = "Authorization", required = false)
        String authorizationHeader,
        @Valid @RequestBody TradeRequest request
    ) {
        return tradeService.sell(
            authorizationHeader,
            request
        );
    }

    @GetMapping("/portfolio")
    public PortfolioResponse getPortfolio(
        @RequestHeader(value = "Authorization", required = false)
        String authorizationHeader
    ) {
        return tradeService.getPortfolio(
            authorizationHeader
        );
    }

    @GetMapping("/trades/history")
    public List<TransactionResponse> getTransactionHistory(
        @RequestHeader(value = "Authorization", required = false)
        String authorizationHeader,
        @RequestParam(defaultValue = "50") int limit
    ) {
        return tradeService.getTransactionHistory(
            authorizationHeader,
            limit
        );
    }
}