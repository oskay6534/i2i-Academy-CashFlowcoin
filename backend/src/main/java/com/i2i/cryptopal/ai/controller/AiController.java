package com.i2i.cryptopal.ai.controller;

import com.i2i.cryptopal.ai.service.AiInsightService;

import com.i2i.cryptopal.ai.dto.AiQueryRequest;
import com.i2i.cryptopal.ai.dto.AiQueryResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiInsightService aiInsightService;

    public AiController(
        AiInsightService aiInsightService
    ) {
        this.aiInsightService = aiInsightService;
    }

    @PostMapping("/query")
    public AiQueryResponse query(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        String authorizationHeader,
        @Valid @RequestBody AiQueryRequest request
    ) {
        return aiInsightService.query(
            authorizationHeader,
            request.question()
        );
    }
}