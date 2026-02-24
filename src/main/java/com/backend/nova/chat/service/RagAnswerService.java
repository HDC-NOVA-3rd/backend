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
        if (rr.get().bestScore() < 0.25) return Optional.empty();

        String system = """
    너는 아파트 관리 챗봇이다.
    반드시 제공된 [근거] 안에서만 답변해라.
    근거에 없는 내용은 "해당 문서에 없습니다." 라고 답해라.
    답변은 한국어로 간결하게.
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