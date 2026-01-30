package com.backend.nova.chat.service;

import com.backend.nova.chat.dto.ChatSuggestionResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatSuggestionService {

    public ChatSuggestionResponse getSuggestions(Long residentId) {
        // - 향후 "입주민 권한/거주 동/호"에 따라 추천질문 다르게 내려주기 쉬움
        // 지금은 일단 공통 추천질문으로 내려줌

        List<ChatSuggestionResponse.SuggestionItem> items = List.of(
                new ChatSuggestionResponse.SuggestionItem("오늘 아파트 행사 있어?", "오늘 아파트 행사 있어?"),
                new ChatSuggestionResponse.SuggestionItem("내 예약 정보 알려줘", "내 예약 정보 알려줘"),
                new ChatSuggestionResponse.SuggestionItem("지금 잡 온도 알려줘", "거실 온도 알려줘"),
                new ChatSuggestionResponse.SuggestionItem("관리비 얼마야?", "관리비 얼마야?"),
                new ChatSuggestionResponse.SuggestionItem("거실 조명 켜줘", "거실 조명 켜줘")
        );

        return new ChatSuggestionResponse(items);
    }
}
