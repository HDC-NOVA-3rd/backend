package com.backend.nova.chat.service;

import com.backend.nova.apartment.entity.Facility;
import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.FacilityRepository;
import com.backend.nova.chat.dto.ChatRequest;
import com.backend.nova.chat.dto.ChatResponse;
import com.backend.nova.chat.dto.LlmCommand;
import com.backend.nova.homeEnvironment.entity.Room;
import com.backend.nova.homeEnvironment.entity.RoomEnvLog;
import com.backend.nova.homeEnvironment.repository.RoomEnvLogRepository;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
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

    // -------------------------
    // Caches (요청량 절감 핵심)
    // -------------------------


    private volatile String systemPromptCache; //한 번 읽고 메모리에 저장.


    private final ConcurrentHashMap<String, CacheEntry> llmCache = new ConcurrentHashMap<>();
    //같은 사람이 같은 질문을 반복하면 LLM을 또 호출하지 않게 하는 캐시.

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
            ResidentRepository residentRepository //필요한 의존성을 만들어서 필드에 저장
    ) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
        this.systemResource = systemResource;
        this.facilityRepository = facilityRepository;
        this.roomRepository = roomRepository;
        this.roomEnvLogRepository = roomEnvLogRepository;
        this.residentRepository = residentRepository;

    }

    public ChatResponse chat(ChatRequest req) {
        String sessionId = (req.sessionId() == null || req.sessionId().isBlank())
                ? UUID.randomUUID().toString()
                : req.sessionId();

        String message = req.message() == null ? "" : req.message().trim(); //null 방지 + 앞뒤 공백 제거.

        // 1) 룰 기반으로 바로 처리 가능한 케이스는 LLM 호출 0회
        LlmCommand ruled = ruleBasedCommand(message);
        if (ruled != null) {
            return routeByIntent(sessionId, req, ruled);
        }

        // 2) 짧은 캐시(예: 60초)로 동일 질문 반복 시 LLM 호출 0회
        String cacheKey = makeCacheKey(req.residentId(), message);
        LlmCommand cached = getCached(cacheKey);
        if (cached != null) {
            return routeByIntent(sessionId, req, cached);
        }

        // 3) 룰도 아니고 캐시도 없으면 그때만 LLM 1회 호출
        String system = readSystemPromptCached();

        String llmRaw;
        try {
            llmRaw = chatClient.prompt()
                    .system(system)
                    .user(message)
                    .call()
                    .content(); //Gemini에게 “system 규칙 + 사용자 message”를 보내고
        } catch (RuntimeException e) {

            // 여기서 429(Quota) 같은 케이스를 서비스에서 잡아서 사용자에게 "잠시 후"를 안내해줄 수 있음
            // (정식으로는 @ControllerAdvice에서 429로 변환 추천)
            String cause = (e.getCause() == null) ? "" : e.getCause().toString();
            if (cause.contains("429") && (cause.contains("Quota exceeded") || cause.contains("rate"))) {
                return new ChatResponse(
                        sessionId,
                        "요청이 너무 많아 잠시 후 다시 시도해 주세요. (API 요청 제한/쿼터)",
                        "RATE_LIMIT",
                        Map.of("retry", "true")
                );
            }
            throw e; //아니면 그냥 예외를 다시 던짐(서버 에러로 올라감).
        }

        LlmCommand cmd = parseOrFallback(llmRaw);
        /*json 코드블록 제거하고
        ObjectMapper로 LlmCommand로 변환.
        실패하면 UNKNOWN + needs_clarification=true로 fallback.*/

        // 캐시 저장(TTL 60초: 팀 프로젝트 테스트 시 중복 질문 방지에 효과 큼)
        putCache(cacheKey, cmd, 60_000); //60초 동안은 같은 질문이면 LLM 재호출 안 함.

        return routeByIntent(sessionId, req, cmd);
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

        String answer = String.format(
                "%s 운영 시간은 %s ~ %s 입니다.",
                facility.getName(),
                facility.getStartHour(),
                facility.getEndHour()
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
