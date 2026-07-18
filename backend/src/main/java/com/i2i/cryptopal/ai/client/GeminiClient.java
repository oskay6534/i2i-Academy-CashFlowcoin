package com.i2i.cryptopal.ai.client;

import com.i2i.cryptopal.common.exception.AiServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GeminiClient {

    private static final Logger log =
        LoggerFactory.getLogger(GeminiClient.class);

    private static final String BASE_URL =
        "https://generativelanguage.googleapis.com";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(
        @Value("${GEMINI_API_KEY:}") String apiKey,
        @Value("${GEMINI_MODEL:gemini-3.5-flash}")
        String model
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank()
            ? "gemini-3.5-flash"
            : model.trim();

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        JdkClientHttpRequestFactory requestFactory =
            new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
            .baseUrl(BASE_URL)
            .requestFactory(requestFactory)
            .build();

        log.info(
            "Gemini configured: model={}, keyPresent={}, keyLength={}",
            this.model,
            !this.apiKey.isBlank(),
            this.apiKey.length()
        );
    }

    public String generate(String prompt) {
        if (apiKey.isBlank()) {
            log.error("GEMINI_API_KEY is missing in the backend process");

            throw new AiServiceUnavailableException(
                "Gemini API key is not configured"
            );
        }

        if (prompt == null || prompt.isBlank()) {
            throw new AiServiceUnavailableException(
                "Gemini prompt is empty"
            );
        }

        /*
         * Keep this request identical to the direct PowerShell test that
         * succeeded: only model and input are sent.
         */
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("input", prompt);

        try {
            JsonNode response = restClient.post()
                .uri("/v1beta/interactions")
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

            String answer = extractAnswer(response);

            if (answer.isBlank()) {
                log.error(
                    "Gemini response did not contain model_output text. Response: {}",
                    response
                );

                throw new AiServiceUnavailableException(
                    "Gemini returned an empty response"
                );
            }

            return answer;
        } catch (AiServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String responseBody = exception.getResponseBodyAsString();

            log.error(
                "Gemini HTTP error. status={}, responseBody={}",
                status,
                responseBody,
                exception
            );

            String message = switch (status) {
                case 400 -> "Gemini rejected the request";
                case 401, 403 -> "Gemini authentication failed";
                case 404 ->
                    "Configured Gemini model or endpoint was not found";
                case 429 -> "Gemini request limit was reached";
                default -> "Gemini service returned an error";
            };

            throw new AiServiceUnavailableException(
                message,
                exception
            );
        } catch (ResourceAccessException exception) {
            log.error(
                "Gemini network error or timeout",
                exception
            );

            throw new AiServiceUnavailableException(
                "Gemini service timed out or could not be reached",
                exception
            );
        } catch (RuntimeException exception) {
            log.error(
                "Gemini response processing failed",
                exception
            );

            throw new AiServiceUnavailableException(
                "Gemini response could not be processed",
                exception
            );
        }
    }

    public String getModel() {
        return model;
    }

    private String extractAnswer(JsonNode response) {
        if (response == null || response.isNull()) {
            return "";
        }

        JsonNode steps = response.path("steps");

        if (!steps.isArray()) {
            return "";
        }

        StringBuilder answer = new StringBuilder();

        for (JsonNode step : steps) {
            if (!"model_output".equals(step.path("type").asText())) {
                continue;
            }

            JsonNode content = step.path("content");

            if (!content.isArray()) {
                continue;
            }

            for (JsonNode item : content) {
                JsonNode textNode = item.path("text");

                if (
                    "text".equals(item.path("type").asText())
                        && !textNode.isMissingNode()
                        && !textNode.isNull()
                        && !textNode.asText().isBlank()
                ) {
                    if (answer.length() > 0) {
                        answer.append(System.lineSeparator());
                    }

                    answer.append(textNode.asText().trim());
                }
            }
        }

        return answer.toString().trim();
    }
}