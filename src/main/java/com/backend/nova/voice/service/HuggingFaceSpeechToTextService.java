package com.backend.nova.voice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "voice.stt", name = "provider", havingValue = "huggingface", matchIfMissing = true)
@Slf4j
public class HuggingFaceSpeechToTextService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${voice.stt.huggingface.api-url:https://router.huggingface.co/hf-inference/models/openai/whisper-large-v3-turbo}")
    private String apiUrl;

    @Value("${voice.stt.huggingface.api-token:}")
    private String apiToken;

    public String transcribe(byte[] audioBytes) {
        if (apiToken == null || apiToken.isBlank()) {
            log.error("Hugging Face STT is selected but token is missing.");
            return "";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken);
            headers.setContentType(MediaType.parseMediaType("audio/wav"));
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(audioBytes, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            String rawResponse = response.getBody();

            String transcript = extractTranscript(rawResponse);
            if (transcript.isBlank()) {
                log.warn("HuggingFace STT returned empty transcript.");
            }
            return transcript;
        } catch (HttpStatusCodeException e) {
            log.error("HuggingFace STT request failed. status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return "";
        } catch (ResourceAccessException e) {
            log.error("HuggingFace STT request timed out or is unreachable.", e);
            return "";
        } catch (Exception e) {
            log.error("HuggingFace STT failed.", e);
            return "";
        }
    }

    private String extractTranscript(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            if (!root.isObject()) {
                log.warn("Unexpected HuggingFace STT response format: {}", rawResponse);
                return "";
            }

            JsonNode textNode = root.get("text");
            if (textNode != null && !textNode.isNull()) {
                return textNode.asText("").trim();
            }

            JsonNode errorNode = root.get("error");
            if (errorNode != null && !errorNode.isNull()) {
                log.warn("HuggingFace STT API returned error payload: {}", errorNode.asText(""));
                return "";
            }

            log.warn("HuggingFace STT response does not contain text: {}", rawResponse);
        } catch (Exception e) {
            log.warn("Failed to parse HuggingFace STT response: {}", rawResponse, e);
        }
        return "";
    }
}
