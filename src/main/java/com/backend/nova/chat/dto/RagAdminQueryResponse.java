package com.backend.nova.chat.dto;

import java.util.List;

public record RagAdminQueryResponse(
        Long apartmentId,
        String sourceType,
        String query,
        int topK,
        List<RagHit> hits
) {

    public static record RagHit(
            String vectorId,
            double score,
            String docId,
            String sourceType,
            String text
    ) {}
}
