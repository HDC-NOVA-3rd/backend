package com.backend.nova.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RagAnswerService {

    private final RagService ragService;
    private final ChatClient chatClient;

    //  스프링이 이 생성자 하나만 보고 주입함
    public RagAnswerService(ChatClient.Builder builder, RagService ragService) {
        this.chatClient = builder.build();
        this.ragService = ragService;
    }

    public Optional<String> tryAnswer(Long memberId, String userMsg, String sourceType) {
        var rr = ragService.retrieve(memberId, userMsg, sourceType);
        if (rr.isEmpty()) return Optional.empty();

        //  점수 컷 완화 (데이터 적으면 0.6 거의 안 넘음)
        if (rr.get().bestScore() < 0.35) return Optional.empty();
        String system = """
너는 스마트 아파트 통합 관리 AI 비서다.

역할:
- 입주민에게 아파트 공지사항, 시설 안내, 운영 시간, 이용 규칙, 행사 정보를 안내한다.
- 반드시 제공된 [근거 문서] 안의 정보만 사용한다.
- 근거에 없는 내용은 절대 추측하지 않는다.
- 근거에 답이 없으면 너가 알아서 잘 대답해줘라.

답변 규칙:
1. 항상 한국어로 답한다.
2. 말투는 친절하고 공지 안내문처럼 명확하게 작성한다.
3. 불필요한 인사말은 생략한다.
4. 날짜, 시간, 장소 정보는 정확히 그대로 전달한다.
5. 목록 정보는 줄바꿈으로 보기 좋게 정리한다.
6. 절대 상상하거나 일반적인 지식을 추가하지 않는다.
7. 외부 정보나 인터넷 지식을 사용하지 않는다.
8. 답변은 50자 이내로 정한다.


너의 정체성:
- 너는 일반 챗봇이 아니라 "아파트 관리 시스템 AI"이다.
- 입주민 생활 편의를 위한 정확한 안내만 제공한다.
""";

        String user = """
    [근거]
    %s

    [질문]
    %s
    """.formatted(rr.get().context(), userMsg);

        String answer = chatClient
                .prompt()
                .system(system)
                .user(user)
                .call()
                .content();

        if (answer == null || answer.trim().isBlank()) return Optional.empty();
        return Optional.of(answer.trim());
    }
}