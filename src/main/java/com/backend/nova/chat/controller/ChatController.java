package com.backend.nova.chat.controller;

// ✅ Controller는 "HTTP 요청/응답"만 담당한다.
//    - 실제 비즈니스 로직(LLM 호출, DB 조회 등)은 Service 계층으로 위임한다.
//    - 요청/응답 데이터 구조는 DTO로 분리한다.

import com.backend.nova.chat.dto.ChatRequest;
import com.backend.nova.chat.dto.ChatResponse;
import com.backend.nova.chat.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/**
 *  ChatController
 * - 챗봇 기능의 "HTTP 진입점(Entry Point)"
 * - 클라이언트 요청을 받아서 Service로 넘기고, 결과를 그대로 응답한다.
 */
@RestController
@RequiredArgsConstructor

public class ChatController {

    private final ChatService chatService;
    @Operation(summary = "대화용 챗봇", description = "챗봇과 대화해 intent/slots 기반으로 처리")

    //  POST /ai/chat 으로 들어오는 요청을 이 메서드가 처리한다.
    @PostMapping("/api/chat")
    public ChatResponse chat(
            //  @RequestBody는 HTTP Body(JSON)를 ChatRequest 객체로 변환해준다.
            // - (Spring이 Jackson 같은 라이브러리로 JSON → 객체 매핑)
            // - 따라서 클라이언트는 {"message":"..."} 같은 형태로 보내게 된다.
            @RequestBody ChatRequest request
    ) {
        return chatService.chat(request);
    }
}
