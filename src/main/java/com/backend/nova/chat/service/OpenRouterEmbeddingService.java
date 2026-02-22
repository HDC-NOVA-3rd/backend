package com.backend.nova.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenRouterEmbeddingService implements EmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openrouter.api-key}")
    private String apiKey;

    @Value("${spring.ai.openrouter.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${spring.ai.openrouter.embedding-model:openai/text-embedding-3-small}")
    private String model;

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("embed input is blank");
        }

        String url = baseUrl + "/embeddings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        EmbeddingRequest body = new EmbeddingRequest(model, text);
        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<EmbeddingResponse> res =
                    restTemplate.exchange(url, HttpMethod.POST, entity, EmbeddingResponse.class);

            EmbeddingResponse r = res.getBody();
            if (r == null || r.data == null || r.data.isEmpty() || r.data.get(0).embedding == null) {
                throw new IllegalStateException("OpenRouter embedding response is empty");
            }
            return r.data.get(0).embedding;

        } catch (RestClientResponseException e) {
            // OpenRouter 에러 바디 확인용
            throw new IllegalStateException(
                    "OpenRouter embeddings failed: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString(), e
            );
        }
    }

    // ===== DTOs =====
    public static class EmbeddingRequest {
        public String model;
        public String input;
        public EmbeddingRequest() {}
        public EmbeddingRequest(String model, String input) {
            this.model = model;
            this.input = input;
        }
    }

    public static class EmbeddingResponse {
        public List<EmbeddingData> data;
    }

    public static class EmbeddingData {
        public List<Float> embedding;
    }
}
