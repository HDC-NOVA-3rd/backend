package com.backend.nova.chat.controller;

import com.backend.nova.chat.dto.ChatMessageResponse;
import com.backend.nova.chat.dto.ChatSessionSummaryResponse;
import com.backend.nova.chat.service.ChatHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;


import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;
    @Operation(
            summary = "이전 대화(세션) 목록 조회",
            description = """
        residentId 기준으로 사용자의 대화 세션 목록을 최신순으로 조회합니다.
        - 앱 '이전 대화' 화면에서 대화방 리스트로 사용합니다.
        - lastMessage/lastMessageAt은 미리보기용 정보입니다.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "residentId 없음")
    })
    @GetMapping("/sessions")
    public List<ChatSessionSummaryResponse> sessions(@RequestParam Long residentId) {
        return chatHistoryService.getSessions(residentId);
    }
    @Operation(
            summary = "특정 세션 메시지 조회",
            description = """
        sessionId에 해당하는 대화 메시지 전체를 시간순(ASC)으로 조회합니다.
        - 사용자가 세션 목록에서 특정 대화방을 선택하면 이 API로 메시지를 복원합니다.
        - 이후 질문은 해당 sessionId로 /ai/chat에 요청하여 대화를 이어갑니다.
        """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "sessionId 없음")
    })
    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageResponse> messages(@PathVariable String sessionId) {
        return chatHistoryService.getMessages(sessionId);
    }
}
