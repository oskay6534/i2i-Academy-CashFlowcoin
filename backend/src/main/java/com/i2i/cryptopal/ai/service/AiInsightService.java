package com.i2i.cryptopal.ai.service;

import com.i2i.cryptopal.ai.client.GeminiClient;

import com.i2i.cryptopal.ai.dto.AiQueryResponse;
import com.i2i.cryptopal.auth.service.SessionService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AiInsightService {

    private final SessionService sessionService;
    private final AiContextService contextService;
    private final GeminiClient geminiClient;

    public AiInsightService(
        SessionService sessionService,
        AiContextService contextService,
        GeminiClient geminiClient
    ) {
        this.sessionService = sessionService;
        this.contextService = contextService;
        this.geminiClient = geminiClient;
    }

    public AiQueryResponse query(
        String authorizationHeader,
        String question
    ) {
        Long userId = sessionService.requireUserId(
            authorizationHeader
        );

        String prompt = contextService.buildPrompt(
            userId,
            question
        );

        String answer = geminiClient.generate(prompt);

        return new AiQueryResponse(
            answer,
            geminiClient.getModel(),
            Instant.now()
        );
    }
}