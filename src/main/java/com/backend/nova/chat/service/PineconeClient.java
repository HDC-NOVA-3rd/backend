package com.backend.nova.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PineconeClient {

    private final RestTemplate restTemplate;

    @Value("${pinecone.api-key}")
    private String apiKey;

    @Value("${pinecone.index-host}")
    private String indexHost;

    @Value("${pinecone.namespace}")
    private String namespace;

    public void upsert(List<Map<String, Object>> vectors) {
        String url = indexHost + "/vectors/upsert";

        Map<String, Object> body = Map.of(
                "namespace", namespace,
                "vectors", vectors
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Pinecone upsert failed: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString(), e
            );
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> query(List<Float> vector, int topK, Map<String, Object> filter) {
        String url = indexHost + "/query";

        Map<String, Object> body = new HashMap<>();
        body.put("namespace", namespace);
        body.put("topK", topK);
        body.put("vector", vector);
        body.put("includeMetadata", true);
        body.put("filter", filter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> res = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return (Map<String, Object>) res.getBody();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Pinecone query failed: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString(), e
            );
        }
    }

    public int deleteByFilter(Map<String, Object> filter) {
        String url = indexHost + "/vectors/delete";

        Map<String, Object> body = new HashMap<>();
        body.put("namespace", namespace);
        body.put("filter", filter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> res =
                    restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> response = res.getBody();
            if (response == null) return 0;

            Object deleted = response.get("deletedCount");
            if (deleted == null) return 0;

            return Integer.parseInt(String.valueOf(deleted));

        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "Pinecone delete failed: "
                            + e.getRawStatusCode() + " "
                            + e.getResponseBodyAsString(),
                    e
            );
        }
    }
}
