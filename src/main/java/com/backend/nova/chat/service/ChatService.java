package com.backend.nova.chat.service;

import com.backend.nova.apartment.entity.Facility;
import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.FacilityRepository;
import com.backend.nova.chat.dto.ChatRequest;
import com.backend.nova.chat.dto.ChatResponse;
import com.backend.nova.chat.dto.LlmCommand;
import com.backend.nova.chat.entity.ChatMessage;
import com.backend.nova.chat.entity.ChatSession;
import com.backend.nova.chat.entity.Role;
import com.backend.nova.chat.repository.ChatMessageRepository;
import com.backend.nova.chat.repository.ChatSessionRepository;
import com.backend.nova.homeEnvironment.entity.Room;
import com.backend.nova.homeEnvironment.entity.RoomEnvLog;
import com.backend.nova.homeEnvironment.repository.RoomEnvLogRepository;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private final ChatClient chatClient; //spring ai
    private final ObjectMapper objectMapper; //llm이 준 json문자열을 자바 객체로 변환
    private final Resource systemResource; //프롬프트

    //intent 처리할때 필요한 DB 조회용
    private final FacilityRepository facilityRepository;
    private final RoomRepository roomRepository;
    private final RoomEnvLogRepository roomEnvLogRepository;
    private final ResidentRepository residentRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    // -------------------------
    // Caches (요청량 절감 핵심)
    // -------------------------


    private volatile String systemPromptCache; //한 번 읽고 메모리에 저장.


    private final ConcurrentHashMap<String, CacheEntry> llmCache = new ConcurrentHashMap<>();
    //같은 사람이 같은 질문을 반복하면 LLM을 또 호출하지 않게 하는 캐시.
    private static final int HISTORY_LIMIT = 20;

    private List<Message> buildHistoryMessages(String sessionId, String systemPrompt) {
        // 1) DB에서 최신 N개 조회(최신순)
        List<ChatMessage> latest = chatMessageRepository
                .findByChatSession_SessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, HISTORY_LIMIT));

        //2) 오래된 -> 최신순으로 뒤집기
        Collections.reverse(latest);

        //3) Spring Ai Message 리스트로 변환
        List<Message>messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));//항상 맨앞

        for (ChatMessage m : latest) {
            if (m.getRole() == null) continue;

            switch (m.getRole()) {
                case USER -> messages.add(new UserMessage(m.getContent()));
                case ASSISTANT -> messages.add(new AssistantMessage(m.getContent()));
                case SYSTEM -> messages.add(new SystemMessage(m.getContent())); // 보통 DB엔 거의 없음
            }
        }
        return messages;
    }

    private static class CacheEntry {
        final long expiresAt; //캐시 만료 시간
        final LlmCommand cmd; //llm결과

        CacheEntry(long expiresAt, LlmCommand cmd) { //캐시에 저장할 값 구조
            this.expiresAt = expiresAt;
            this.cmd = cmd;
        }
    }

    public ChatService(
            ChatClient.Builder builder, //실제 ChatClient 만들어서 주입
            ObjectMapper objectMapper,//
            @Value("classpath:prompt/chat-system.st") Resource systemResource,
            FacilityRepository facilityRepository,
            RoomRepository roomRepository,
            RoomEnvLogRepository roomEnvLogRepository,
            ResidentRepository residentRepository, ChatSessionRepository chatSessionRepository, ChatMessageRepository chatMessageRepository //필요한 의존성을 만들어서 필드에 저장
    ) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
        this.systemResource = systemResource;
        this.facilityRepository = facilityRepository;
        this.roomRepository = roomRepository;
        this.roomEnvLogRepository = roomEnvLogRepository;
        this.residentRepository = residentRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }


    @Transactional
    public ChatResponse chat(ChatRequest req) {

        // 0) 세션 확보
        ChatSession session = getOrCreateSession(req.sessionId(), req.residentId());
        String sessionId = session.getSessionId();

        String message = req.message() == null ? "" : req.message().trim();

        // 1) USER 메시지 저장 (대화 로그)
        saveMessage(session, Role.USER, message);

        // 2) 기존 로직 그대로 (룰 → 캐시 → LLM)
        LlmCommand ruled = ruleBasedCommand(message);
        if (ruled != null) {
            ChatResponse res = routeByIntent(sessionId, req, ruled);
            saveMessage(session, Role.ASSISTANT, res.answer()); //  응답 저장
            return res;
        }

        String cacheKey = makeCacheKey(req.residentId(), message);
        LlmCommand cached = getCached(cacheKey);
        if (cached != null) {
            ChatResponse res = routeByIntent(sessionId, req, cached);
            saveMessage(session, Role.ASSISTANT, res.answer());
            return res;
        }

        String system = readSystemPromptCached();

        //history 포함 메시지 만들기
        List<Message> messages = buildHistoryMessages(sessionId, system);
        // 1) messages() 지원 버전
        String llmRaw = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

        LlmCommand cmd = parseOrFallback(llmRaw);
        putCache(cacheKey, cmd, 60_000);

        ChatResponse res = routeByIntent(sessionId, req, cmd);

        // 3) ASSISTANT 메시지 저장
        saveMessage(session, Role.ASSISTANT, res.answer());

        return res;
    }

    // =========================
    // Routing
    // =========================

    private ChatResponse routeByIntent(String sessionId, ChatRequest req, LlmCommand cmd) {
        if (cmd == null) {
            return new ChatResponse(sessionId, "요청을 처리할 수 없습니다.", "UNKNOWN", Map.of());
        }

        if (cmd.needs_clarification()) {
            return new ChatResponse(sessionId, cmd.clarify_question(), cmd.intent(), cmd.slots());
        }// llm이 정보부족이라고 판단을 하면 되묻는 질문만 계속함

        //핸들러
        return switch (cmd.intent()) {
            case "FACILITY_INFO" -> handleFacilityInfo(sessionId, req, cmd);
            case "ENV_STATUS" -> handleEnvStatus(sessionId, req, cmd);
            default -> new ChatResponse(sessionId, cmd.reply(), cmd.intent(), cmd.slots());
        };
    }
    private ChatSession getOrCreateSession(String sessionId, Long residentId) {

        // 1) sessionId가 있으면: 기존 세션 조회
        if (sessionId != null && !sessionId.isBlank()) {
            return chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 sessionId 입니다: " + sessionId));
        }

        // 2) sessionId가 없으면: 새 세션 생성
        if (residentId == null || residentId <= 0) {
            throw new IllegalArgumentException("새 세션 생성에는 residentId가 필요합니다.");
        }

        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("입주민을 찾을 수 없습니다: " + residentId));

        ChatSession s = new ChatSession();
        s.setSessionId(UUID.randomUUID().toString());
        s.setResident(resident);
        s.setStatus("ACTIVE");

        LocalDateTime now = LocalDateTime.now();
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        s.setLastMessageAt(now);

        return chatSessionRepository.save(s);
    }


    private void saveMessage(ChatSession session, Role role, String content){
        ChatMessage m = new ChatMessage();
        m.setChatSession(session);
        m.setRole(role);
        m.setContent(content);
        m.setCreatedAt(LocalDateTime.now());
        chatMessageRepository.save(m);

        //세션 활동 시간 갱신
        LocalDateTime now = LocalDateTime.now();
        session.setLastMessageAt(now);
        session.setUpdatedAt(now);
        if (session.getStatus() == null)
            session.setStatus("ACTIVE");
        chatSessionRepository.save(session);
    }


    // =========================
    // Rule-based (LLM 0회 처리)
    // =========================
    private LlmCommand ruleBasedCommand(String message) {
        if (message == null) return null;
        String m = message.trim();
        if (m.isEmpty()) return new LlmCommand(
                "UNKNOWN",
                "메시지를 입력해 주세요.",
                Map.of(),
                true,
                "예: '헬스장 운영시간 알려줘', '거실 온도 알려줘'"
        );



        // ---- ENV_STATUS 룰 ----
        // 방 이름(필요하면 추가)
        String room = null;
        if (containsAny(m, "거실")) room = "거실";
        else if (containsAny(m, "침실", "안방")) room = "침실";   // 안방을 침실로 매핑(원하면 별도 처리)
        else if (containsAny(m, "부엌", "주방")) room = "주방";
        else if (containsAny(m, "화장실", "욕실")) room = "화장실";

        // 센서 타입
        String sensorType = null;
        if (containsAny(m, "온도", "temperature")) sensorType = "TEMP";
        else if (containsAny(m, "습도", "humidity")) sensorType = "HUMIDITY";
        else if (containsAny(m, "조도", "밝기", "light")) sensorType = "LIGHT";

        // 환경 조회 의도가 보이면 바로 처리
        if (room != null && sensorType != null) {
            return new LlmCommand(
                    "ENV_STATUS",
                    "",
                    Map.of("room", room, "sensor_type", sensorType),
                    false,
                    ""
            );
        }

        // ---- FACILITY_INFO 룰 ----
        // 시설명(필요하면 추가)
        String facility = null;
        if (containsAny(m, "헬스장", "피트니스")) facility = "헬스장";
        else if (containsAny(m, "미팅룸")) facility = "미팅룸";
        else if (containsAny(m, "독서실", "스터디룸", "스터디")) facility = "스터디룸";

        boolean asksTime = containsAny(m, "운영", "시간", "몇 시", "언제", "오픈", "마감");
        if (facility != null && asksTime) {
            return new LlmCommand(
                    "FACILITY_INFO",
                    "",
                    Map.of("facility", facility),
                    false,
                    ""
            );
        }

        return null; // 룰로 못 잡으면 LLM로
    }
    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (k != null && !k.isBlank() && text.contains(k)) return true;
        }
        return false;
    }
    // =========================
    // intent handlers
    // =========================

    private ChatResponse handleFacilityInfo(String sessionId, ChatRequest req, LlmCommand cmd) {
        Ho ho = resolveHo(req.residentId());
        Long apartmentId = resolveApartmentId(ho);

        String facilityName = safeString(cmd.slots().get("facility"));
        if (facilityName.isBlank()) {
            return new ChatResponse(
                    sessionId,
                    "어떤 시설 운영시간을 조회할까요? (예: 헬스장, 수영장)",
                    "FACILITY_INFO",
                    Map.of("needs", "facility")
            );
        }

        Facility facility = facilityRepository
                .findByApartmentIdAndName(apartmentId, facilityName)
                .orElseThrow(() -> new IllegalArgumentException("시설 정보를 찾을 수 없습니다: " + facilityName));
        boolean reservable = facility.isReservationAvailable();

        String reservableText = reservable ? "현재 예약 가능합니다." : "현재 예약이 불가능합니다.";

        String answer = String.format(
                "%s 운영 시간은 %s ~ %s 입니다.",
                facility.getName(),
                facility.getStartHour(),
                facility.getEndHour(),
                reservableText
        );

        return new ChatResponse(
                sessionId,
                answer,
                "FACILITY_INFO",
                Map.of(
                        "facility", facility.getName(),
                        "startHour", facility.getStartHour(),
                        "endHour", facility.getEndHour(),
                        "apartmentId", apartmentId,
                        "description", facility.getDescription()
                )
        );
    }

    private ChatResponse handleEnvStatus(String sessionId, ChatRequest req, LlmCommand cmd) {
        Ho ho = resolveHo(req.residentId());

        String roomName = safeString(cmd.slots().get("room"));          // 예: 거실
        String sensorType = safeString(cmd.slots().get("sensor_type")); // 예: TEMP / HUMID / LIGHT

        if (roomName.isBlank() || sensorType.isBlank()) {
            return new ChatResponse(
                    sessionId,
                    "어느 방의 어떤 값을 조회할까요? (예: '거실 온도 알려줘')",
                    "ENV_STATUS",
                    Map.of("needs", "room,sensor_type")
            );
        }

        // 1) ho + roomName 으로 Room 찾기
        Room room = (Room) roomRepository.findByHo_IdAndName(ho.getId(), roomName)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다: " + roomName));

        // 2) 최신 로그 1건
        RoomEnvLog log = (RoomEnvLog) roomEnvLogRepository
                .findTop1ByRoomId_IdAndSensorTypeOrderByRecordedAtDesc(room.getId(), sensorType)
                .orElseThrow(() -> new IllegalArgumentException("환경 로그가 없습니다: " + roomName + " / " + sensorType));

        String unit = safeString(log.getUnit());
        String answer = String.format(
                "%s %s는 현재 %d%s 입니다.",
                roomName,
                prettySensor(sensorType),
                log.getSensorValue(),
                unit
        );

        return new ChatResponse(
                sessionId,
                answer,
                "ENV_STATUS",
                Map.of(
                        "room", roomName,
                        "sensorType", sensorType,
                        "value", log.getSensorValue(),
                        "unit", log.getUnit(),
                        "recordedAt", log.getRecordedAt()
                )
        );
    }

    // =========================
    // auth/user context helpers
    // =========================
    private Ho resolveHo(Long residentId) {
        if (residentId == null) throw new IllegalArgumentException("residentId가 없습니다.");
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("입주민을 찾을 수 없습니다: " + residentId));

        if (resident.getHo() == null) throw new IllegalArgumentException("해당 입주민에 ho 정보가 없습니다.");
        return resident.getHo();
    }

    private Long resolveApartmentId(Ho ho) {
        return ho.getDong().getApartment().getId();
    }

    private String prettySensor(String sensorType) {
        return switch (sensorType) {
            case "TEMP" -> "온도";
            case "HUMIDITY" -> "습도";
            case "LIGHT" -> "조도";
            default -> sensorType;
        };
    }

    // =========================
    // 프롬프트 파싱 
    // =========================

    private String readSystemPromptCached() {
        if (systemPromptCache != null) return systemPromptCache; //한번 읽은 적 있으면 바로 메모리값 반환
        synchronized (this) {// 동시에 요청이 들어오면 한 스레드만 읽게 하는 보장
            if (systemPromptCache != null) return systemPromptCache; //이중 체크
            systemPromptCache = readSystemPrompt(); //실제 파일 캐쉬에 저장 후 반환
            return systemPromptCache;
        } //외부에서 호출하는 메인 진입점
    }

    private String readSystemPrompt() {
        try {
            return StreamUtils.copyToString(systemResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("system prompt 파일(chat-system.st)을 읽지 못했습니다.", e);
        }// UTF-8 문자열로 변환 하는 작업
    }

    private String safeString(Object v) { //슬롯 값 안전 처리
        return v == null ? "" : String.valueOf(v).trim();
    } //LLM이 주는 slots 전부 object

    private LlmCommand parseOrFallback(String llmRaw) { //llm이 준 텍스트 응답 -> 자바 객체로 변환 하는 작업
        try {
            String cleaned = cleanJson(llmRaw);
            return objectMapper.readValue(cleaned, LlmCommand.class);
        } catch (Exception e) {
            return new LlmCommand(//JSON 깨졌거나 구조가 다를 때
                    "UNKNOWN",
                    "죄송해요. 요청을 이해하지 못했어요. 조금만 더 구체적으로 말해줄래요?",
                    Map.of("raw", llmRaw),
                    true,
                    "어떤 기능을 원하세요? (예: '거실 온도 알려줘', '헬스장 운영시간 알려줘')"
            );
        }
    }

    private String cleanJson(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?s)```json", "")
                .replaceAll("(?s)```", "")
                .trim();
    } //LLM 응답 정리용


    // =========================
    // LLM Cache helpers
    // =========================

    private String makeCacheKey(Long residentId, String message) {
        String rid = (residentId == null) ? "anon" : String.valueOf(residentId); //사용자 ID가 없으면 "anon"으로 처리 (익명 사용자)
        return rid + ":" + normalizeMessage(message);
    }

    private String normalizeMessage(String message) {
        if (message == null) return "";
        // 공백/개행 정도만 정리 (팀플 테스트 중 "같은 질문" 캐시 히트율 올리기)
        return message.trim().replaceAll("\\s+", " "); //"거실 온도\n알려줘" → "거실 온도 알려줘"
    }

    private LlmCommand getCached(String key) { //캐시에서 꺼내는 함수
        CacheEntry e = llmCache.get(key);// 캐시에서 저장된 값을 가져옴
        if (e == null) return null;//없으면 ai 호출
        if (System.currentTimeMillis() > e.expiresAt) {
            llmCache.remove(key);
            return null;
        }
        return e.cmd;
    }

    private void putCache(String key, LlmCommand cmd, long ttlMs) {
        llmCache.put(key, new CacheEntry(System.currentTimeMillis() + ttlMs, cmd)); //현재 시간 + TTL(유효 시간)을 계산해서 expiresAt 설정
    }
}
