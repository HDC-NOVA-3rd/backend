package com.backend.nova.chat.dto;

import java.util.Map;

/**
 *  LlmCommand
 * - LLM(Gemini, GPT 등)으로부터 "구조화된 결과"를 받기 위한 DTO
 * - 자연어 응답이 아니라, 백엔드가 처리 가능한 명령(Command) 형태
 *
 * 핵심 아이디어:
 *  LLM은 "말을 잘하는 역할"
 * 백엔드는 "결정하고 실행하는 역할"
 *
 * 이 DTO는 그 경계를 명확히 해준다.
 */
public record LlmCommand(


        String intent, //llm이 사용자의 발화를 해석할 결과

        String reply, //LLM이 생성한 "자연어 응답 문장"

        /**
         *  slots
         * - intent를 실행하기 위해 필요한 세부 정보(파라미터)
         *
         * 예시:
         * {
         *   "device": "AIRCON",
         *   "metric": "TEMPERATURE",
         *   "location": "LIVING_ROOM"
         * }
         *
         *  전통적인 챗봇의 slot filling 개념
         *  LLM을 "의미 추출기"로 사용하는 포인트
         */
        Map<String, Object> slots,
        /**
         *  needs_clarification
         * - 사용자의 요청이 모호해서
         *   추가 질문이 필요한지 여부
         *
         * 예시:
         * - "불 좀 켜줘" → 어느 방?
         * - "온도 알려줘" → 어느 집/어느 공간?
         *
         * true라면:
         * - 바로 실행 ❌
         * - clarify_question을 사용자에게 반환
         */
        boolean needs_clarification,

        /**
         *  clarify_question
         * - needs_clarification == true 일 때
         *   사용자에게 다시 물어볼 질문
         *
         * 예시:
         * - "어느 방의 불을 켜드릴까요?"
         * - "거실 온도를 확인할까요?"
         *
         *  LLM이 '대화 진행자' 역할을 하는 부분
         */
        String clarify_question
) {}
