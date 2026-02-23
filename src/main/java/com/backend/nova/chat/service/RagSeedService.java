package com.backend.nova.chat.service;

import com.backend.nova.chat.dto.RagDocInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RagSeedService {

    private final EmbeddingService embeddingService;
    private final PineconeClient pineconeClient;

    // ✅ 커스텀 문서 seed (RULE / EVENT / FAQ 등)
    public int seedApartmentDocs(Long apartmentId, String sourceType, List<RagDocInput> docs) {
        if (apartmentId == null) return 0;
        if (sourceType == null || sourceType.isBlank()) sourceType = "GUIDE";
        if (docs == null || docs.isEmpty()) return 0;

        List<Map<String, Object>> vectors = new ArrayList<>();
        int count = 0;

        for (RagDocInput doc : docs) {
            String docId = (doc.docId() == null || doc.docId().isBlank())
                    ? UUID.randomUUID().toString()
                    : doc.docId();

            String title = safe(doc.title(), "제목 없음");
            String content = safe(doc.content(), "").trim();
            if (content.isBlank()) continue;

            String fullText = formatDoc(sourceType, title, content);
            List<String> chunks = chunkText(fullText, 600, 80);

            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                List<Float> emb = embeddingService.embed(chunk);

                String vectorId = "doc_%d_%s_%s_%d".formatted(apartmentId, sourceType, docId, i);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("apartmentId", apartmentId);
                metadata.put("sourceType", sourceType);
                metadata.put("docId", docId);
                metadata.put("title", title);
                metadata.put("text", chunk);

                Map<String, Object> v = new HashMap<>();
                v.put("id", vectorId);
                v.put("values", emb);
                v.put("metadata", metadata);

                vectors.add(v);
                count++;
            }
        }

        if (!vectors.isEmpty()) {
            pineconeClient.upsert(vectors); //  네 PineconeClient 시그니처에 정확히 맞음
        }

        return count;
    }

    private String formatDoc(String sourceType, String title, String content) {
        return """
               [%s]
               제목: %s

               내용:
               %s
               """.formatted(sourceType, title, content).trim();
    }

    private List<String> chunkText(String text, int maxChars, int overlapChars) {
        text = safe(text, "").trim();
        if (text.isBlank()) return List.of();

        String[] lines = text.split("\\R+");
        List<String> chunks = new ArrayList<>();

        StringBuilder buf = new StringBuilder();
        for (String line : lines) {
            if (buf.length() + line.length() + 1 > maxChars) {
                chunks.add(buf.toString().trim());
                buf = new StringBuilder(tail(buf.toString(), overlapChars));
            }
            buf.append(line).append("\n");
        }
        if (!buf.toString().trim().isBlank()) chunks.add(buf.toString().trim());

        return chunks;
    }

    private String tail(String s, int n) {
        if (s == null) return "";
        if (n <= 0) return "";
        return s.length() <= n ? s : s.substring(s.length() - n);
    }

    private String safe(String s, String def) {
        return s == null ? def : s;
    }

    // -----------------------
    //  (옵션) notices seed 별칭
    // -----------------------
    public int seedApartmentNotices(Long apartmentId) {
        // TODO: 너 기존 공지 seed 메서드명으로 연결해
        // 예) return seedNoticesInternal(apartmentId);
        throw new UnsupportedOperationException("seedApartmentNotices 연결 필요: 기존 공지 seed 메서드로 위임해줘");
    }

    // RagSeedService.java (추가)
    public int deleteApartmentDoc(Long apartmentId, String sourceType, String docId) {
        // Pinecone metadata 기준 필터 삭제 (청크 전체 삭제)
        Map<String, Object> filter = new HashMap<>();
        filter.put("apartmentId", apartmentId);
        filter.put("sourceType", sourceType);
        filter.put("docId", docId);

        return pineconeClient.deleteByFilter(filter);
    }
}