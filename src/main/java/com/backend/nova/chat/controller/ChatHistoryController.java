package com.backend.nova.chat.controller;

import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.chat.dto.ChatMessageResponse;
import com.backend.nova.chat.dto.ChatSessionSummaryResponse;
import com.backend.nova.chat.service.ChatHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    @Operation(
            summary = "이전 대화 세션 목록 조회",
            description = """
    로그인 사용자(memberId)를 기준으로 사용자의 대화 세션 목록을 최신순으로 조회합니다.

    [사용 목적]
    - 앱/웹의 '이전 대화 목록' 화면에 사용
    - 각 세션은 대화방 단위입니다.

    [포함 정보]
    - sessionId : 대화 세션 식별자
    - lastMessage : 마지막 메시지 미리보기
    - lastMessageAt : 마지막 대화 시간
    - status : 세션 상태

    [주의]
    - 삭제된(soft delete) 세션은 조회되지 않습니다.
    - memberId는 요청 파라미터로 받지 않으며, Access Token에서 추출합니다.
    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대화 세션 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 memberId의 세션이 존재하지 않음")
    })
    @GetMapping("/sessions")
    public List<ChatSessionSummaryResponse> sessions(
            @AuthenticationPrincipal MemberDetails user
    ) {
        return chatHistoryService.getSessions(user.getMemberId());
    }

    @Operation(
            summary = "특정 대화 세션 메시지 조회",
            description = """
    sessionId에 해당하는 대화 메시지를 시간순(ASC)으로 조회합니다.

    [사용 흐름]
    1. 사용자가 이전 대화 목록에서 특정 세션 선택
    2. 해당 API 호출로 메시지 전체 복원
    3. 이후 질문은 동일한 sessionId로 대화 이어짐

    [검증 로직]
    - memberId와 sessionId가 일치해야 조회 가능
    - 삭제된 세션은 조회 불가
    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 삭제된 세션")
    })
    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageResponse> messages(
            @AuthenticationPrincipal MemberDetails user,
            @PathVariable String sessionId
    ) {
        return chatHistoryService.getMessages(user.getMemberId(), sessionId);
    }

    @Operation(
            summary = "대화 세션 단일 삭제",
            description = """
    특정 sessionId에 해당하는 대화 세션을 삭제(soft delete) 처리합니다.

    [동작 방식]
    - 실제 데이터는 삭제하지 않고 deleted_at 값을 설정합니다.
    - 삭제된 세션은 이후 조회되지 않습니다.

    [권한 검증]
    - 로그인 사용자(memberId)의 세션만 삭제 가능합니다.
    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "세션 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "세션이 없거나 이미 삭제됨")
    })
    @DeleteMapping("/sessions/{sessionId}")
    public void deleteSession(
            @AuthenticationPrincipal MemberDetails user,
            @PathVariable String sessionId
    ) {
        chatHistoryService.deleteSession(user.getMemberId(), sessionId);
    }

    @Operation(
            summary = "대화 세션 전체 삭제",
            description = """
    로그인 사용자(memberId)에 해당하는 모든 대화 세션을 삭제(soft delete) 처리합니다.

    [사용 예]
    - '전체 대화 삭제' 버튼
    - 테스트 데이터 초기화

    [특징]
    - 메시지는 직접 삭제하지 않으며
    - 세션 기준으로 숨김 처리됩니다.
    - memberId는 요청 파라미터로 받지 않으며, Access Token에서 추출합니다.
    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "전체 세션 삭제 성공")
    })
    @DeleteMapping("/sessions")
    public void deleteAll(
            @AuthenticationPrincipal MemberDetails user
    ) {
        chatHistoryService.deleteAllSessions(user.getMemberId());
    }
}
