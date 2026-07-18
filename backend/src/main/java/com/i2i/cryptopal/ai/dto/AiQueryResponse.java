package com.i2i.cryptopal.ai.dto;

import java.time.Instant;

public record AiQueryResponse(
    String answer,
    String model,
    Instant generatedAt
) {
}