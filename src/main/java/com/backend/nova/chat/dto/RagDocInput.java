package com.backend.nova.chat.dto;

public record RagDocInput(
        String docId,     // "RULE_001" 같은 문서 식별자(없으면 null 가능)
        String title,     // 문서 제목
        String content    // 문서 본문(길어도 됨, seed에서 chunk 됨)
) {}