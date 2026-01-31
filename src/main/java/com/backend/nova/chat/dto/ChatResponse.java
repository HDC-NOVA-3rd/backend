package com.backend.nova.chat.dto;

/**
 *
 * ChatResponse
 * - 서버 → 클라이언트로 반환되는 "챗봇 응답 DTO"
 * - 챗봇이 단순 문자열만 반환하는 것이 아니라,
 * "의도 + 응답 메시지 + 추가 데이터"를 함께 내려주기 위한 구조
 */
public record ChatResponse(


        String sessionId,//사용자와 챗봇 서버 간의 일시적인 대화 연결을 식별하는 고유한 ID

        String answer, // 사용자에게 직접 보여줄 텍스트 응답

        String intent, //사용자의 요청을 시스템이 어떻게 해석했는지에 대한 결과

        Object data //intent마다 내려줄 데이터 구조가 다르기 때문 ,intent 처리 결과로 나온 "부가 정보"
) {
}
