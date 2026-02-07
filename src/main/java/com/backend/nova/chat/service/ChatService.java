package com.backend.nova.chat.service;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.facility.entity.Facility;
import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.facility.repository.FacilityRepository;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.apartment.service.ApartmentWeatherService;
import com.backend.nova.chat.dto.*;
import com.backend.nova.chat.entity.ChatMessage;
import com.backend.nova.chat.entity.ChatSession;
import com.backend.nova.chat.entity.DeviceCommandLog;
import com.backend.nova.chat.entity.Role;
import com.backend.nova.chat.repository.ChatMessageRepository;
import com.backend.nova.chat.repository.ChatSessionRepository;
import com.backend.nova.chat.repository.DeviceCommandLogRepository;
import com.backend.nova.homeEnvironment.entity.Room;
import com.backend.nova.homeEnvironment.entity.RoomEnvLog;
import com.backend.nova.homeEnvironment.repository.RoomEnvLogRepository;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.weather.dto.OpenWeatherResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ApartmentWeatherService apartmentWeatherService;
    private final ApartmentRepository apartmentRepository;
    private final DongRepository dongRepository;
    private final HoRepository hoRepository;
    private final MemberRepository memberRepository;
    private final DeviceCommandLogRepository deviceCommandLogRepository;
    private final MessageChannel mqttAssistantOutboundChannel;



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
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            ApartmentWeatherService apartmentWeatherService, ApartmentRepository apartmentRepository, DongRepository dongRepository, HoRepository hoRepository, MemberRepository memberRepository, DeviceCommandLogRepository deviceCommandLogRepository, MessageChannel mqttAssistantOutboundChannel//필요한 의존성을 만들어서 필드에 저장
    ) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
        this.systemResource = systemResource;
        this.facilityRepository = facilityRepository;
        this.roomRepository = roomRepository;
        this.roomEnvLogRepository = roomEnvLogRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.apartmentWeatherService = apartmentWeatherService;
        this.apartmentRepository = apartmentRepository;
        this.dongRepository = dongRepository;
        this.hoRepository = hoRepository;
        this.memberRepository = memberRepository;
        // mqtt 제어용
        this.deviceCommandLogRepository = deviceCommandLogRepository;
        this.mqttAssistantOutboundChannel = mqttAssistantOutboundChannel;
    }

    // mqtt 통신
    @Transactional
    public ChatResponse handleDeviceControl(String sessionId, Long memberId, LlmCommand cmd) {

        Ho ho = resolveHo(memberId);
        Long hoId = ho.getId();

        String roomName = safeString(cmd.slots().get("room"));
        String deviceType = safeString(cmd.slots().get("device_type"));
        String action = safeString(cmd.slots().get("action"));
        Integer value = (cmd.slots().get("value") instanceof Number n) ? n.intValue() : null;

        if (roomName.isBlank() || deviceType.isBlank() || action.isBlank()) {
            return new ChatResponse(sessionId, "어느 방의 어떤 기기를 제어할까요?", "DEVICE_CONTROL", Map.of());
        }

        Room room = (Room) roomRepository.findByHo_IdAndName(hoId, roomName)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다: " + roomName));

        // TODO: 나중에 DeviceRepository 붙이면 여기서 deviceId 찾기
        Long deviceId = 1L;

        String command = toMqttCommand(deviceType, action, value);

        String traceId = UUID.randomUUID().toString();
        deviceCommandLogRepository.save(
                DeviceCommandLog.pending(traceId, memberId, hoId, room.getId(), deviceId, command)
        );

        String topic = "hdc/" + hoId + "/assistant/execute/req";
        ExecuteCommandReq payload = new ExecuteCommandReq(traceId, command);

        org.springframework.messaging.Message<String> message = MessageBuilder
                .withPayload(writeJson(payload))
                .setHeader(MqttHeaders.TOPIC, topic)
                .build();

        mqttAssistantOutboundChannel.send(message);

        String reply = buildControlReply(roomName, deviceType, action, value);

        return new ChatResponse(
                sessionId,
                reply,
                "DEVICE_CONTROL",
                Map.of("traceId", traceId)
        );
    }

    private String writeJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("JSON serialize failed", e);
        }
    }

    @Transactional
    public ChatResponse chat(ChatRequest req) {

        // 1. 세션 조회 또는 생성
        ChatSession session = getOrCreateSession(req.sessionId(), req.memberId());
        String sessionId = session.getSessionId();

        // 2. 사용자 메시지 저장
        saveMessage(session, Role.USER, req.message());

        // 3. 룰 기반 먼저 시도
        LlmCommand cmd = ruleBasedCommand(req.message());

        // 4. 룰로 못 잡으면 LLM 호출
        if (cmd == null) {
            String systemPrompt = readSystemPromptCached();
            List<Message> history = buildHistoryMessages(sessionId, systemPrompt);

            String llmRaw = chatClient
                    .prompt()
                    .messages(history)
                    .user(req.message())
                    .call()
                    .content();

            cmd = parseOrFallback(llmRaw);
        }

        // 5. intent 라우팅
        ChatResponse response = routeByIntent(sessionId, req, cmd);

        // 6. 어시스턴트 메시지 저장
        saveMessage(session, Role.ASSISTANT, response.answer());

        return response;
    }


    private String toMqttCommand(String deviceType, String action, Integer value) {
        // 지금은 command를 문자열로 단순화 (Edge랑 합의해서 바꾸면 됨)
        if ("LED".equalsIgnoreCase(deviceType)) {
            if ("ON".equalsIgnoreCase(action)) return "LIGHT_ON";
            if ("OFF".equalsIgnoreCase(action)) return "LIGHT_OFF";
        }
        if ("AIRCON".equalsIgnoreCase(deviceType)) {
            if ("ON".equalsIgnoreCase(action)) return "AIRCON_ON";
            if ("OFF".equalsIgnoreCase(action)) return "AIRCON_OFF";
            if ("SET_TEMP".equalsIgnoreCase(action)) {
                if (value == null) throw new IllegalArgumentException("SET_TEMP requires value");
                return "AIRCON_SET_TEMP:" + value;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 명령: " + deviceType + "/" + action);
    }

    private String buildControlReply(String room, String deviceType, String action, Integer value) {
        String devKo = "LED".equalsIgnoreCase(deviceType) ? "전등" : "AIRCON".equalsIgnoreCase(deviceType) ? "에어컨" : deviceType;
        return switch (action) {
            case "ON" -> room + " " + devKo + "을 켤게요.";
            case "OFF" -> room + " " + devKo + "을 끌게요.";
            case "SET_TEMP" -> room + " 에어컨 온도를 " + value + "도로 설정할게요.";
            default -> "요청을 처리할게요.";
        };
    }

    // =========================
    // Routing (intent → handler)
    // =========================

    private ChatResponse routeByIntent(String sessionId, ChatRequest req, LlmCommand cmd) {
        if (cmd == null) {
            return new ChatResponse(sessionId, "요청을 처리할 수 없습니다.", "UNKNOWN", Map.of());
        }
        // LLM이 정보가 부족하다고 판단한 경우
        if (cmd.needs_clarification()) {
            return new ChatResponse(sessionId, cmd.clarify_question(), cmd.intent(), cmd.slots());
        }// llm이 정보부족이라고 판단을 하면 되묻는 질문만 계속함

        // intent별 실제 처리 로직 분기
        return switch (cmd.intent()) {
            // 단지 별 날씨
            case "APARTMENT_WEATHER" -> handleApartmentWeather(sessionId, req);
            // 멤버 아이디 별 자기 정보
            case "MY_PROFILE", "MY_MEMBER" -> handleMyMember(sessionId, req);
            // 자기 아파트
            case "MY_APARTMENT" -> handleMyApartment(sessionId, req);
            // 동,호 조회
            case "MY_DONG_HO" -> handleMyDongHo(sessionId, req);
            // 아파트 동,호
            case "APARTMENT_DONG_LIST" -> handleApartmentDongList(sessionId, req);
            case "DONG_HO_LIST" -> handleDongHoList(sessionId, req, cmd);
            // 시설 정보 조회
            case "FACILITY_INFO" -> handleFacilityInfo(sessionId, req, cmd);
            //아파트 시설 목록 조회
            case "FACILITY_LIST" -> handleFacilityList(sessionId, req);
            // 집 내부 센서 데이터 조회
            case "ENV_STATUS" -> handleEnvStatus(sessionId, req, cmd);
            // 등록된 방 조회
            case "ROOM_LIST" -> handleRoomList(sessionId, req);
            // 최근 환경 변화 조회
            case "ENV_HISTORY" -> handleEnvHistory(sessionId, req, cmd);
            // 단지 별 날씨 조회
            case "FREE_CHAT" -> new ChatResponse(
                    sessionId,
                    cmd.reply(),
                    "FREE_CHAT",
                    Map.of()
            );
            // 명령어를 통한 디바이스 제어
            case "DEVICE_CONTROL" -> handleDeviceControl(sessionId, req.memberId(), cmd);

            default -> new ChatResponse(sessionId, cmd.reply(), cmd.intent(), cmd.slots());
        };
    }
    private ChatSession getOrCreateSession(String sessionId, Long memberId) {

        if (sessionId != null && !sessionId.isBlank()) {
            ChatSession s = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 sessionId 입니다: " + sessionId));

            // (선택) 보안: memberId가 넘어오면 소유자 검증
            if (memberId != null && memberId > 0 && !s.getMember().getId().equals(memberId)) {
                throw new IllegalArgumentException("세션 소유자가 일치하지 않습니다.");
            }
            return s;
        }

        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("새 세션 생성에는 memberId 필요합니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberId));

        ChatSession s = new ChatSession();
        s.setSessionId(UUID.randomUUID().toString());
        s.setMember(member);
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
        // ---- MY_* 룰 ----
        if (containsAny(m, "내 정보", "내 프로필", "내 주민", "내 입주민")) {
            return new LlmCommand("MY_PROFILE", "", Map.of(), false, "");
        }
        if (containsAny(m, "내 아파트", "아파트 정보")) {
            return new LlmCommand("MY_APARTMENT", "", Map.of(), false, "");
        }
        if (containsAny(m, "내 동", "내 호", "동호", "동/호")) {
            return new LlmCommand("MY_DONG_HO", "", Map.of(), false, "");
        }
        if (containsAny(m, "동 목록", "동 리스트")) {
            return new LlmCommand("APARTMENT_DONG_LIST", "", Map.of(), false, "");
        }
        if (containsAny(m, "호 목록", "호수 목록", "호 리스트")) {
            // dongId를 물어봐야 할 수도 있지만, 기본은 "내 동" 기준으로 보여주면 UX가 좋음
            return new LlmCommand("DONG_HO_LIST", "", Map.of("dong_source", "MY"), false, "");
        }


        // ---- ENV_STATUS 룰 ----
        // 방 이름(필요하면 추가)
        // ---- ENV_STATUS 룰 ----
        if (containsAny(m, "기록", "이력", "추이", "최근")) {
            return null;
        }

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
        // =========================
// DEVICE_CONTROL 룰 (LLM 없이 제어)
// =========================

// 방 이름 추출(너가 이미 위에서 room 변수를 만들고 있으니 재사용 가능)
        String ctrlRoom = null;
        if (containsAny(m, "거실")) ctrlRoom = "거실";
        else if (containsAny(m, "침실", "안방")) ctrlRoom = "침실";
        else if (containsAny(m, "부엌", "주방")) ctrlRoom = "주방";
        else if (containsAny(m, "화장실", "욕실")) ctrlRoom = "화장실";

// 디바이스 타입 추출
        String deviceType = null;
        if (containsAny(m, "전등", "불", "조명", "등")) deviceType = "LED";
        else if (containsAny(m, "에어컨", "냉방", "난방")) deviceType = "AIRCON";

// action 추출
        String action = null;
        if (containsAny(m, "켜", "켜줘", "켜 줘", "on", "틀어", "틀어줘")) action = "ON";
        else if (containsAny(m, "꺼", "꺼줘", "꺼 줘", "off", "끄", "꺼줘")) action = "OFF";
        else if (containsAny(m, "맞춰", "설정", "바꿔", "올려", "내려")) {
            // 에어컨 온도 제어에서 주로 씀
            action = "SET_TEMP";
        }

// 온도 값 추출(예: "24도", "24도로")
        Integer tempValue = null;
        if (deviceType != null && "AIRCON".equals(deviceType)) {
            java.util.regex.Matcher mt = java.util.regex.Pattern
                    .compile("(\\d{1,2})\\s*도")
                    .matcher(m);
            if (mt.find()) {
                tempValue = Integer.parseInt(mt.group(1));
            }
        }

// DEVICE_CONTROL로 판정 조건
        boolean looksControl = (ctrlRoom != null && deviceType != null && action != null);

// 온도 설정인데 값이 없으면 되묻게
        if (looksControl && "SET_TEMP".equals(action) && tempValue == null) {
            return new LlmCommand(
                    "DEVICE_CONTROL",
                    "",
                    Map.of(
                            "room", ctrlRoom,
                            "device_type", deviceType,
                            "action", "SET_TEMP"
                    ),
                    true,
                    "몇 도로 설정할까요? (예: 24도)"
            );
        }

// 정상 제어 명령
        if (looksControl) {
            Map<String, Object> slots = new HashMap<>();
            slots.put("room", ctrlRoom);
            slots.put("device_type", deviceType);
            slots.put("action", action);
            if (tempValue != null) slots.put("value", tempValue);

            return new LlmCommand(
                    "DEVICE_CONTROL",
                    "",
                    slots,
                    false,
                    ""
            );
        }


        // ---- FACILITY_INFO 룰 ----
        // 시설명(필요하면 추가)
        // ---- FACILITY_INFO 룰 ----
        String facility = null;
        if (containsAny(m, "헬스장", "피트니스")) facility = "헬스장";
        else if (containsAny(m, "미팅룸")) facility = "미팅룸";
        else if (containsAny(m, "독서실", "스터디룸", "스터디")) facility = "스터디룸";

    // info_type 분류
        String infoType = null;
        boolean asksHours = containsAny(m, "운영", "시간", "몇 시", "언제", "오픈", "마감");
        boolean asksAvailable = containsAny(m, "예약 가능", "예약돼", "예약 되", "가능해", "예약할 수", "예약");
        boolean asksDesc = containsAny(m, "설명", "소개", "어디", "위치", "층", "어딨어");

        if (asksHours) infoType = "HOURS";
        else if (asksAvailable) infoType = "AVAILABLE";
        else if (asksDesc) infoType = "DESCRIPTION";

    // facility가 있고, 시설 관련 의도가 보이면 FACILITY_INFO로 처리
        if (facility != null) {
            // infoType이 없으면 서버에서 한 번 더 판단하거나 되묻게 처리
            return new LlmCommand(
                    "FACILITY_INFO",
                    "",
                    Map.of(
                            "facility", facility,
                            "info_type", infoType == null ? "UNKNOWN" : infoType
                    ),
                    false,
                    ""
            );
        }
        if (containsAny(m, "날씨", "외부", "기온", "공기질", "미세먼지")) {
            return new LlmCommand("APARTMENT_WEATHER", "", Map.of(), false, "");
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

    // 로그인한 사용자의 입주민(member) 기본 정보를 조회한다.

    private ChatResponse handleMyMember(String sessionId, ChatRequest req) {
        // 1) memberId 입주민 조회
        Member member = memberRepository.findById(req.memberId())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

        Ho ho = resolveHo(req.memberId());

        Long apartmentId = resolveApartmentId(ho);

        // 3) 프론트에서 바로 쓰기 좋은 형태로 응답 구성
        return new ChatResponse(
                sessionId,
                "내 입주민 정보입니다.",
                "MY_MEMBER",
                Map.of(
                        "member", Map.of(
                                "memberId", member.getId(),
                                "name", safeString(member.getName()),
                                "birthday", safeString(member.getBirthDate()),
                                    "phone", safeString(member.getPhoneNumber())
                        ),
                        "apartmentId", apartmentId
                )
        );
    }
    // 내가 살고 있는 아파트의 기본 정보를 조회한다.  memberId → ho → apartmentId 흐름을 따른다.


    private ChatResponse handleMyApartment(String sessionId, ChatRequest req) {

        // 1) MemberId 기준으로 내가 속한 apartmentId 추출
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);

        // 2) apartment 조회
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("아파트 정보가 없습니다."));
        // 3) 응답 구성
        return new ChatResponse(
                sessionId,
                "내 아파트 정보입니다.",
                "MY_APARTMENT",
                Map.of(
                        "apartment", Map.of(
                                "apartmentId", apartment.getId(),
                                "name", safeString(apartment.getName()),
                                "address", safeString(apartment.getAddress()),
                                "latitude", apartment.getLatitude(),
                                "longitude", apartment.getLongitude()
                        )
                )
        );
    }
    // MY_DONG_HO
    // 사용자가 현재 거주 중인 동(dong)과 호(ho) 정보를 반환한다.

    private ChatResponse handleMyDongHo(String sessionId, ChatRequest req) {
        // 1) memberId → ho
        Ho ho = resolveHo(req.memberId());

        // 2) ho → dong
        Dong dong = ho.getDong(); // lazy면 dongRepository로 조회해도 됨

        // 3) 응답
        return new ChatResponse(
                sessionId,
                "내 동/호 정보입니다.",
                "MY_DONG_HO",
                Map.of(
                        "dong", Map.of(
                                "dongId", dong.getId(),
                                "dongNo", safeString(dong.getDongNo())
                        ),
                        "ho", Map.of(
                                "hoId", ho.getId(),
                                "hoNo", safeString(ho.getHoNo())
                        )
                )
        );
    }
    /* APARTMENT_DONG_LIST
     * - 내가 속한 아파트(apartmentId)의 전체 동 목록을 조회한다.
     * - memberId만 있으면 서버가 apartmentId를 자동으로 해석한다.*/

    private ChatResponse handleApartmentDongList(String sessionId, ChatRequest req) {

        // 1) memberId → apartmentId
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);

        // 2) 해당 아파트에 속한 모든 동 조회
        List<Dong> dongs = dongRepository.findAllByApartmentId(apartmentId);

        // 3) 프론트 친화적 데이터 구조로 변환
        List<Map<String, Object>> payload = dongs.stream()
                .map(d -> Map.<String, Object>of(
                        "dongId", d.getId(),
                        "dongNo", safeString(d.getDongNo())
                ))
                .toList();
        // 4) 응답
        String answer = payload.isEmpty()
                ? "등록된 동 정보가 없습니다."
                : "우리 아파트 동 목록입니다.";

        return new ChatResponse(
                sessionId,
                answer,
                "APARTMENT_DONG_LIST",
                Map.of(
                        "apartmentId", apartmentId,
                        "dongs", payload
                )
        );
    }

    /**
     * DONG_HO_LIST
     * - 기본 동작: 내가 살고 있는 동의 호 목록 조회
     * - 확장 가능: 특정 dongId를 지정해서 조회 가능
     */
    private ChatResponse handleDongHoList(String sessionId, ChatRequest req, LlmCommand cmd) {

        // 1) 기본은 "내 동"
        Ho myHo = resolveHo(req.memberId());
        Long dongId = myHo.getDong().getId();

        // (확장) cmd.slots()에 dongId가 있으면 그걸로 조회도 가능
        Object dongIdSlot = cmd.slots().get("dongId");
        if (dongIdSlot instanceof Number n) {
            dongId = n.longValue();
        }
        // 3) 해당 동의 호 목록 조회
        List<Ho> hos = hoRepository.findAllByDongId(dongId);

        List<Map<String, Object>> payload = hos.stream()
                .map(h -> Map.<String, Object>of(
                        "hoId", h.getId(),
                        "hoNo", safeString(h.getHoNo())
                ))
                .toList();

        String answer = payload.isEmpty()
                ? "해당 동의 호 정보가 없습니다."
                : "호 목록입니다.";

        return new ChatResponse(
                sessionId,
                answer,
                "DONG_HO_LIST",
                Map.of(
                        "dongId", dongId,
                        "hos", payload
                )
        );
    }





    private ChatResponse handleFacilityList(String sessionId, ChatRequest req) {
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);

        List<Facility> facilities = facilityRepository.findAllByApartmentId(apartmentId);

        if (facilities.isEmpty()) {
            return new ChatResponse(
                    sessionId,
                    "등록된 시설이 없습니다.",
                    "FACILITY_LIST",
                    Map.of("facilities", List.of())
            );
        }

        List<Map<String, Object>> payload = facilities.stream()
                .map(f -> Map.<String, Object>of(
                        "facilityId", f.getId(),
                        "name", f.getName(),
                        "startHour", f.getStartHour(),
                        "endHour", f.getEndHour(),
                        "reservationAvailable", f.isReservationAvailable(),
                        "description", f.getDescription()
                ))
                .toList();

        String names = facilities.stream()
                .map(Facility::getName)
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        String answer = "우리 아파트 시설은 " + names + " 입니다.";

        return new ChatResponse(
                sessionId,
                answer,
                "FACILITY_LIST",
                Map.of(
                        "apartmentId", apartmentId,
                        "facilities", payload
                )
        );
    }

    private ChatResponse handleEnvHistory(
            String sessionId,
            ChatRequest req,
            LlmCommand cmd
    ) {
        String roomName = (String) cmd.slots().get("room");
        String sensorType = (String) cmd.slots().get("sensor_type");
        Integer limit = (Integer) cmd.slots().getOrDefault("limit", 10);

        Ho ho = resolveHo(req.memberId());

        Room room = (Room) roomRepository
                .findByHo_IdAndName(ho.getId(), roomName)
                .orElseThrow(() -> new IllegalArgumentException("해당 방이 없습니다."));

        Pageable pageable = PageRequest.of(0, limit);

        List<RoomEnvLog> logs =
                roomEnvLogRepository.findByRoom_IdAndSensorTypeOrderByRecordedAtDesc(
                        room.getId(),
                        sensorType,
                        pageable
                );

        if (logs.isEmpty()) {
            return new ChatResponse(
                    sessionId,
                    "해당 조건의 환경 기록이 없습니다.",
                    "ENV_HISTORY",
                    Map.of()
            );
        }

        List<Map<String, Object>> data = logs.stream()
                .map(l -> Map.<String, Object>of(
                        "value", l.getSensorValue(),
                        "unit", l.getUnit(),
                        "recordedAt", l.getRecordedAt()
                ))
                .toList();

        String answer = roomName + "의 최근 "
                + limit + "개 "
                + sensorTypeToKorean(sensorType)
                + " 기록입니다.";

        return new ChatResponse(
                sessionId,
                answer,
                "ENV_HISTORY",
                Map.of(
                        "room", roomName,
                        "sensorType", sensorType,
                        "logs", data
                )
        );
    }
    private String sensorTypeToKorean(String type) {
        return switch (type) {
            case "TEMP" -> "온도";
            case "HUMID" -> "습도";
            case "CO2" -> "이산화탄소";
            case "GAS" -> "가스";
            case "LIGHT" -> "조도";
            default -> "환경";
        };
    }


    private ChatResponse handleRoomList(String sessionId, ChatRequest req) {
        Ho ho = resolveHo(req.memberId()); // 너 코드에 이미 존재하는 패턴 :contentReference[oaicite:2]{index=2}

        List<Room> rooms = roomRepository.findAllByHo_Id(ho.getId());

        if (rooms.isEmpty()) {
            return new ChatResponse(
                    sessionId,
                    "등록된 방 정보가 없습니다.",
                    "ROOM_LIST",
                    Map.of("rooms", List.of())
            );
        }

        List<Map<String, Object>> payload = rooms.stream()
                .map(r -> Map.<String, Object>of(
                        "roomId", r.getId(),
                        "name", r.getName()
                ))
                .toList();

        String roomNames = rooms.stream()
                .map(Room::getName)
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        String answer = "현재 등록된 방은 " + roomNames + " 입니다.";

        return new ChatResponse(
                sessionId,
                answer,
                "ROOM_LIST",
                Map.of("rooms", payload)
        );
    }



    private ChatResponse handleFacilityInfo(String sessionId, ChatRequest req, LlmCommand cmd) {
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);

        String facilityName = safeString(cmd.slots().get("facility"));
        String infoType = safeString(cmd.slots().get("info_type")); // HOURS / AVAILABLE / DESCRIPTION

        if (facilityName.isBlank() || "UNKNOWN".equalsIgnoreCase(facilityName)) {
            return new ChatResponse(
                    sessionId,
                    "어느 시설을 확인할까요? (헬스장/스터디룸/수영장...)",
                    "FACILITY_INFO",
                    Map.of("facility", "UNKNOWN", "info_type", "UNKNOWN")
            );
        }

        // info_type이 비었으면 서버에서 한번 더 추정(LLM/룰 실수 방지)
        if (infoType.isBlank() || "UNKNOWN".equalsIgnoreCase(infoType)) {
            String m = req.message() == null ? "" : req.message();
            if (containsAny(m, "운영", "시간", "몇 시", "언제", "오픈", "마감")) infoType = "HOURS";
            else if (containsAny(m, "예약", "가능", "예약 가능", "예약돼", "예약 되")) infoType = "AVAILABLE";
            else if (containsAny(m, "설명", "소개", "어디", "위치", "층")) infoType = "DESCRIPTION";
            else {
                // 정말 모호하면 되묻기
                return new ChatResponse(
                        sessionId,
                        "운영시간/예약가능/설명 중 어떤 정보를 확인할까요?",
                        "FACILITY_INFO",
                        Map.of("facility", facilityName, "info_type", "UNKNOWN")
                );
            }
        }

        Facility facility = facilityRepository
                .findByApartmentIdAndName(apartmentId, facilityName)
                .orElseThrow(() -> new IllegalArgumentException("시설 정보를 찾을 수 없습니다: " + facilityName));

        LocalTime now = LocalTime.now();
        LocalTime start = facility.getStartHour();
        LocalTime end = facility.getEndHour();

        boolean isOpenNow;
        if (end.isAfter(start) || end.equals(start)) {
            isOpenNow = !now.isBefore(start) && !now.isAfter(end);
        } else {
            isOpenNow = !now.isBefore(start) || !now.isAfter(end);
        }

        boolean reservationAvailable = facility.isReservationAvailable();
        boolean reservableNow = isOpenNow && reservationAvailable;

        //  info_type별 답변 분기
        String answer;
        Map<String, Object> data = new HashMap<>();
        data.put("facility", facility.getName());
        data.put("info_type", infoType);
        data.put("apartmentId", apartmentId);

        data.put("startHour", start);
        data.put("endHour", end);
        data.put("isOpenNow", isOpenNow);
        data.put("reservationAvailable", reservationAvailable);
        data.put("reservableNow", reservableNow);
        switch (infoType) {
            case "HOURS" -> {
                answer = String.format(
                        "%s 운영시간은 %s~%s 입니다. (현재: %s)",
                        facility.getName(),
                        start,
                        end,
                        isOpenNow ? "운영 중" : "운영 시간 아님"
                );
                data.put("startHour", start);
                data.put("endHour", end);
                data.put("isOpenNow", isOpenNow);
            }
            case "AVAILABLE" -> {
                answer = String.format(
                        "%s은(는) %s. %s",
                        facility.getName(),
                        reservableNow ? "현재 예약 가능합니다" : "현재 예약이 불가능합니다",
                        reservationAvailable ? "" : "예약 기능이 제공되지 않는 시설입니다"
                ).trim();
                data.put("reservationAvailable", reservationAvailable);
                data.put("isOpenNow", isOpenNow);
                data.put("reservableNow", reservableNow);
                data.put("startHour", start);
                data.put("endHour", end);
            }
            case "DESCRIPTION" -> {
                String desc = safeString(facility.getDescription());
                if (desc.isBlank()) desc = "등록된 설명이 없습니다.";
                answer = String.format("%s 설명: %s", facility.getName(), desc);
                data.put("description", desc);
            }
            default -> {
                answer = "운영시간/예약가능/설명 중 어떤 정보를 확인할까요?";
                data.put("startHour", start);
                data.put("endHour", end);
                data.put("reservationAvailable", reservationAvailable);
                data.put("description", safeString(facility.getDescription()));
            }
        }

        return new ChatResponse(sessionId, answer, "FACILITY_INFO", data);
    }



    private ChatResponse handleEnvStatus(String sessionId, ChatRequest req, LlmCommand cmd) {
        Ho ho = resolveHo(req.memberId());

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
                .findFirstByRoom_IdAndSensorTypeOrderByRecordedAtDesc(room.getId(), sensorType)
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
    private ChatResponse handleApartmentWeather(String sessionId, ChatRequest req) {

        // 1) memberId → ho → apartmentId
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);

        // 2) 기존 서비스 그대로 재사용
        OpenWeatherResponse weather =
                apartmentWeatherService.getApartmentWeather(apartmentId);


        // 3) Chat 응답 구성
        String answer = String.format(
                "현재 외부 날씨는 %s이며, 기온은 %d°C, 습도는 %d%% 입니다. 공기질은 %s 입니다.",
                weather.condition(),
                weather.temperature(),
                weather.humidity(),
                weather.airQuality()
        );

        return new ChatResponse(
                sessionId,
                answer,
                "APARTMENT_WEATHER",
                Map.of(
                        "apartmentId", apartmentId,
                        "weather", weather
                )
        );
    }




    // =========================
    // auth/user context helpers
    // =========================
    private Ho resolveHo(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId가 없습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberId));

        if (member.getResident() == null) {
            throw new IllegalArgumentException("해당 회원에 resident 정보가 없습니다.");
        }

        if (member.getResident().getHo() == null) {
            throw new IllegalArgumentException("해당 입주민에 ho 정보가 없습니다.");
        }

        return member.getResident().getHo();
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

    private String makeCacheKey(Long memberId, String message) {
        String rid = (memberId == null) ? "anon" : String.valueOf(memberId); //사용자 ID가 없으면 "anon"으로 처리 (익명 사용자)
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
