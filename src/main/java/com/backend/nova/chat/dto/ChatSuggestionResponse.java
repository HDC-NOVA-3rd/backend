package com.backend.nova.chat.dto;

import java.util.List;

public record ChatSuggestionResponse(
        List<SuggestionItem> suggestion
) {
    public record SuggestionItem(
            String label, //실제 버튼에 표시될 문구
            String message //실제 /api/chat으로 보낼 문구
    ){}
}
