package com.i2i.cryptopal.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiQueryRequest(
    @NotBlank(message = "Question is required")
    @Size(
        min = 3,
        max = 1000,
        message = "Question must contain 3-1000 characters"
    )
    String question
) {
}