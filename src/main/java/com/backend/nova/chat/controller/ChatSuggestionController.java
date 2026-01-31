package com.backend.nova.chat.controller;

import com.backend.nova.chat.dto.ChatSuggestionResponse;
import com.backend.nova.chat.service.ChatSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatSuggestionController {

    private final ChatSuggestionService chatSuggestionService;

    @Operation(
            summary = "추천 질문 목록 조회",
            description = """
                    챗봇 하단 '추천 질문' 버튼에 노출할 문구 리스트를 반환합니다.
                    - label: 버튼에 표시될 텍스트
                    - message: 버튼 클릭 시 /api/chat 으로 전송할 실제 메시지
                    """
    )
    @GetMapping("/suggestions")
    public ChatSuggestionResponse suggestions(
            @RequestParam(required = false) Long residentId
    ) {
        return chatSuggestionService.getSuggestions(residentId);
    }
}
