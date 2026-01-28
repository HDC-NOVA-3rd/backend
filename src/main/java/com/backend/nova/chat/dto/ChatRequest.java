package com.backend.nova.chat.dto;

/**
 *  ChatRequest
 * - 클라이언트 → 서버로 전달되는 "챗봇 요청 데이터"
 * - HTTP Request Body(JSON)를 자바 객체로 매핑하기 위한 DTO
 *
 * 이 DTO는 "사용자가 무엇을 말했는지" 뿐만 아니라
 * "누가", "어떤 대화 흐름(session)"에서 말했는지까지 포함한다.
 */
public record ChatRequest(

        String message,
        String sessionId,
        Long residentId
) {



}
