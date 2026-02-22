package com.backend.nova.chat.service;


import com.backend.nova.chat.dto.RagAdminQueryResponse;
import com.backend.nova.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RagService {
    private final EmbeddingService embeddingService;
    private final PineconeClient pineconeClient;
    private final MemberRepository memberRepository; // 또는 apartmentId 뽑는 기존 서비스

    public Optional<RagResult> retrieve(Long memberId, String question, String sourceType) {
        var loc = memberRepository.findApartmentIdByMemberId(memberId).orElse(null);
        if (loc == null) return Optional.empty();

        Long apartmentId = loc.apartmentId();
        List<Float> qEmb = embeddingService.embed(question);

        Map<String, Object> filter = new HashMap<>();
        filter.put("apartmentId", apartmentId);
        if (sourceType != null && !sourceType.isBlank()) {
            filter.put("sourceType", sourceType);
        }

        Map<String, Object> res = pineconeClient.query(qEmb, 5, filter);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches = (List<Map<String, Object>>) res.get("matches");
        if (matches == null || matches.isEmpty()) return Optional.empty();

        StringBuilder contextBuilder = new StringBuilder();
        int maxLen = 2500;

        for (Map<String, Object> match : matches) {
            @SuppressWarnings("unchecked")
            Map<String, Object> md = (Map<String, Object>) match.get("metadata");
            if (md == null) continue;

            String text = String.valueOf(md.get("text"));
            if (text == null || text.isBlank()) continue;

            if (contextBuilder.length() + text.length() > maxLen) break;
            contextBuilder.append(text).append("\n");
        }

        String context = contextBuilder.toString().trim();
        if (context.isBlank()) return Optional.empty();

        double bestScore = Double.parseDouble(String.valueOf(matches.get(0).get("score")));
        return Optional.of(new RagResult(context, bestScore));
    }
    public RagAdminQueryResponse adminQuery(
            Long apartmentId,
            String sourceType,
            String question,
            int topK
    ) {
        List<Float> qEmb = embeddingService.embed(question);

        Map<String, Object> filter = new HashMap<>();
        filter.put("apartmentId", apartmentId);
        if (sourceType != null && !sourceType.isBlank()) {
            filter.put("sourceType", sourceType);
        }

        Map<String, Object> res = pineconeClient.query(qEmb, topK, filter);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matches =
                (List<Map<String, Object>>) res.get("matches");

        if (matches == null) matches = List.of();

        List<RagAdminQueryResponse.RagHit> hits = matches.stream().map(match -> {
            Map<String, Object> md =
                    (Map<String, Object>) match.get("metadata");

            return new RagAdminQueryResponse.RagHit(
                    String.valueOf(match.get("id")),
                    Double.parseDouble(String.valueOf(match.get("score"))),
                    md != null ? String.valueOf(md.get("docId")) : null,
                    md != null ? String.valueOf(md.get("sourceType")) : null,
                    md != null ? String.valueOf(md.get("text")) : null
            );
        }).toList();

        return new RagAdminQueryResponse(
                apartmentId,
                sourceType,
                question,
                topK,
                hits
        );
    }




    public record RagResult(String context, double bestScore) {}
}
