package com.backend.nova.voice.service;

import com.backend.nova.voice.dto.VoiceAudioCommandRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "voice.stt", name = "provider", havingValue = "huggingface")
public class HuggingFaceSpeechToTextService implements SpeechToTextService {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceSpeechToTextService.class);

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${voice.stt.huggingface.api-url:https://api-inference.huggingface.co/models/openai/whisper-large-v3}")
    private String apiUrl;

    @Value("${voice.stt.huggingface.api-token:}")
    private String apiToken;

    @Value("${voice.stt.huggingface.timeout-seconds:90}")
    private long timeoutSeconds;

    @Override
    public String transcribe(MultipartFile audioFile, VoiceAudioCommandRequest request) {
        if (request.mockText() != null && !request.mockText().isBlank()) {
            return request.mockText().trim();
        }

        if (apiToken == null || apiToken.isBlank()) {
            log.error("Hugging Face STT is selected but token is missing. deviceId={}", request.deviceId());
            return "";
        }

        try {
            String contentType = normalizeAudioContentType(audioFile.getContentType());
            byte[] payload = audioFile.getBytes();

            String rawResponse = webClientBuilder.build()
                    .post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .contentType(MediaType.parseMediaType(contentType))
                    .bodyValue(payload)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new IllegalStateException(
                                            "HuggingFace STT request failed: " + response.statusCode() + ", body=" + body
                                    )))
                    )
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            String transcript = extractTranscript(rawResponse);
            if (transcript.isBlank()) {
                log.warn("HuggingFace STT returned empty transcript. deviceId={}, file={}",
                        request.deviceId(), audioFile.getOriginalFilename());
            }
            return transcript;
        } catch (Exception e) {
            log.error("HuggingFace STT failed. deviceId={}, file={}",
                    request.deviceId(), audioFile.getOriginalFilename(), e);
            return "";
        }
    }

    private String normalizeAudioContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "audio/wav";
        }
        return contentType;
    }

    private String extractTranscript(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(rawResponse);

            if (root.isObject()) {
                JsonNode textNode = root.get("text");
                if (textNode != null && !textNode.isNull()) {
                    return textNode.asText("").trim();
                }
                JsonNode generatedTextNode = root.get("generated_text");
                if (generatedTextNode != null && !generatedTextNode.isNull()) {
                    return generatedTextNode.asText("").trim();
                }
                JsonNode errorNode = root.get("error");
                if (errorNode != null && !errorNode.isNull()) {
                    log.warn("HuggingFace STT API returned error payload: {}", errorNode.asText(""));
                }
                return "";
            }

            if (root.isArray() && !root.isEmpty()) {
                JsonNode first = root.get(0);
                if (first.isObject()) {
                    JsonNode textNode = first.get("text");
                    if (textNode != null && !textNode.isNull()) {
                        return textNode.asText("").trim();
                    }
                    JsonNode generatedTextNode = first.get("generated_text");
                    if (generatedTextNode != null && !generatedTextNode.isNull()) {
                        return generatedTextNode.asText("").trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse HuggingFace STT response: {}", rawResponse, e);
        }
        return "";
    }
}
