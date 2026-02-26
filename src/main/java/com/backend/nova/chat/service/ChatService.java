package com.backend.nova.chat.service;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.bill.dto.BillSummaryResponse;
import com.backend.nova.bill.service.BillService;
import com.backend.nova.complaint.entity.Complaint;
import com.backend.nova.complaint.entity.ComplaintAnswer;
import com.backend.nova.complaint.repository.ComplaintAnswerRepository;
import com.backend.nova.complaint.repository.ComplaintRepository;
import com.backend.nova.facility.entity.Facility;
import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.facility.entity.Space;
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
import com.backend.nova.facility.repository.SpaceRepository;
import com.backend.nova.homeEnvironment.dto.DeviceStateUpdateRequest;
import com.backend.nova.homeEnvironment.entity.Device;
import com.backend.nova.homeEnvironment.entity.DeviceType;
import com.backend.nova.homeEnvironment.entity.Room;
import com.backend.nova.homeEnvironment.entity.RoomEnvLog;
import com.backend.nova.homeEnvironment.repository.DeviceRepository;
import com.backend.nova.homeEnvironment.repository.RoomEnvLogRepository;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import com.backend.nova.homeEnvironment.service.DeviceStateService;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.notice.entity.Notice;
import com.backend.nova.notice.entity.NoticeTargetScope;
import com.backend.nova.notice.repository.NoticeRepository;
import com.backend.nova.notice.repository.NoticeTargetDongRepository;
import com.backend.nova.reservation.dto.ReservationResponse;
import com.backend.nova.reservation.repository.ReservationRepository;
import com.backend.nova.reservation.service.ReservationService;
import com.backend.nova.weather.dto.OpenWeatherResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient; //spring ai
    private final ObjectMapper objectMapper; //llm이 준 json문자열을 자바 객체로 변환
    private final Resource systemResource; //프롬프트
    private final RagAnswerService ragAnswerService;
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
    private final MessageChannel mqttOutboundChannel;
    private final SpaceRepository spaceRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeTargetDongRepository noticeTargetDongRepository;
    private final ComplaintAnswerRepository complaintAnswerRepository;
    private final ComplaintRepository  complaintRepository;
    private final ReservationService reservationService;
    private final DeviceStateService deviceStateService;
    private final DeviceRepository deviceRepository;
    private final BillService billService;
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
            @Value("classpath:prompt/chat-system.st") Resource systemResource, RagAnswerService ragAnswerService,
            FacilityRepository facilityRepository,
            RoomRepository roomRepository,
            RoomEnvLogRepository roomEnvLogRepository,
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            ApartmentWeatherService apartmentWeatherService, ApartmentRepository apartmentRepository,
            DongRepository dongRepository, HoRepository hoRepository,
            MemberRepository memberRepository, DeviceCommandLogRepository deviceCommandLogRepository,
            MessageChannel mqttOutboundChannel, SpaceRepository spaceRepository, NoticeRepository noticeRepository,
            NoticeTargetDongRepository noticeTargetDongRepository, ComplaintAnswerRepository complaintAnswerRepository,
            ComplaintRepository complaintRepository, ReservationRepository reservationRepository, ReservationService reservationService, DeviceStateService deviceStateService, DeviceRepository deviceRepository, BillService billService//필요한 의존성을 만들어서 필드에 저장
    ) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
        this.systemResource = systemResource;
        this.ragAnswerService = ragAnswerService;
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
        this.spaceRepository = spaceRepository;
        this.noticeRepository = noticeRepository;
        this.noticeTargetDongRepository = noticeTargetDongRepository;
        this.complaintAnswerRepository = complaintAnswerRepository;
        this.complaintRepository = complaintRepository;
        this.reservationService = reservationService;
        // mqtt 제어용
        this.deviceCommandLogRepository = deviceCommandLogRepository;
        this.mqttOutboundChannel = mqttOutboundChannel;
        this.deviceStateService = deviceStateService;
        this.deviceRepository = deviceRepository;
        this.billService = billService;
    }

    @Transactional
    public ChatResponse handleDeviceControl(String sessionId, Long memberId, LlmCommand cmd) {


        Ho ho = resolveHo(memberId);
        Long hoId = ho.getId();

        String roomName = safeString(cmd.slots().get("room"));
        String deviceTypeStr = safeString(cmd.slots().get("device_type"));
        String action = safeString(cmd.slots().get("action"));
        Integer value = (cmd.slots().get("value") instanceof Number n) ? n.intValue() : null;
        log.info("[DEVICE_CONTROL] sessionId={} room='{}' deviceType='{}' action='{}' value={}",
                sessionId, roomName, deviceTypeStr, action, value);

        if (roomName.isBlank() || deviceTypeStr.isBlank() || action.isBlank()) {
            return new ChatResponse(sessionId,
                    "어느 방의 어떤 기기를 제어할까요?",
                    "DEVICE_CONTROL",
                    Map.of());
        }

        //  방 조회
        Room room = (Room) roomRepository.findByHo_IdAndName(hoId, roomName)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다: " + roomName));

        //  deviceType → ENUM 변환
        DeviceType deviceType;
        try {
            deviceType = DeviceType.valueOf(normalizeDeviceCode(deviceTypeStr));
        } catch (Exception e) {
            throw new IllegalArgumentException("지원하지 않는 기기 타입: " + deviceTypeStr);
        }

        //  실제 Device 조회 (type 기준)
        Optional<Device> deviceOpt =
                deviceRepository.findByRoom_IdAndType(room.getId(), deviceType);

        if (deviceOpt.isEmpty()) {
            return new ChatResponse(
                    sessionId,
                    roomName + "에 해당 기기가 없습니다.",
                    "DEVICE_CONTROL",
                    Map.of()
            );
        }
        Device device = deviceOpt.get();

        //  진짜 DB의 device_code (light-1 같은 값)
        String realDeviceCode = device.getDeviceCode();

        log.info("[DEVICE_CONTROL] roomName={} resolvedRoomId={} deviceCode={}",
                roomName,
                room.getId(),
                realDeviceCode);



        //  MQTT 명령 생성 (type 기준)
        String command = toMqttCommand(deviceType.name(), action, value);

        String traceId = UUID.randomUUID().toString();

        deviceCommandLogRepository.save(
                DeviceCommandLog.pending(
                        traceId,
                        memberId,
                        hoId,
                        room.getId(),
                        device.getId(),
                        command
                )
        );


        // 1. rn_worker에서 사용하는 실제 deviceCode
        String deviceCode = realDeviceCode; // ex) light-1, fan-1-2

        // 2. rn_worker 규격 command/value 변환
        MqttCmd mv = toRoomMqtt(deviceType, action, value);

        // 3. room 기반 토픽으로 변경
        String topic = "hdc/" + hoId + "/room/" + room.getId() + "/device/execute/req";

        RoomDeviceExecuteReq payload = new RoomDeviceExecuteReq(
                traceId,
                deviceCode,
                mv.command(),
                mv.value()
        );

        org.springframework.messaging.Message<String> message = MessageBuilder
                .withPayload(writeJson(payload))
                .setHeader(MqttHeaders.TOPIC, topic)
                .build();

        mqttOutboundChannel.send(message);

        //  DB 상태 즉시 반영 (⭐ 핵심)
        DeviceStateUpdateRequest patch =
                buildPatch(realDeviceCode, deviceType, action, value);

        deviceStateService.patchDevicesState(room.getId(), patch);

        String reply = buildControlReply(roomName, deviceTypeStr, action, value);

        return new ChatResponse(
                sessionId,
                reply,
                "DEVICE_CONTROL",
                Map.of("traceId", traceId)
        );
    }
    private record MqttCmd(String command, Object value) {}

    private MqttCmd toRoomMqtt(DeviceType deviceType, String action, Integer value) {

        if (deviceType == DeviceType.LED) {
            if ("ON".equalsIgnoreCase(action))
                return new MqttCmd("POWER", "ON");

            if ("OFF".equalsIgnoreCase(action))
                return new MqttCmd("POWER", "OFF");

            if ("SET_BRIGHTNESS".equalsIgnoreCase(action)) {
                int v = Math.max(0, Math.min(100, value == null ? 0 : value));
                return new MqttCmd("BRIGHTNESS", v);
            }
        }

        if (deviceType == DeviceType.FAN) {
            if ("ON".equalsIgnoreCase(action))
                return new MqttCmd("POWER", "ON");

            if ("OFF".equalsIgnoreCase(action))
                return new MqttCmd("POWER", "OFF");
        }


        throw new IllegalArgumentException("지원하지 않는 명령: " + deviceType + "/" + action);
    }


    private DeviceStateUpdateRequest buildPatch(
            String realDeviceCode, DeviceType deviceType, String action, Integer value
    ) {
        Boolean power = null;
        Integer brightness = null;
        Integer targetTemp = null;

        if (deviceType == DeviceType.LED) {
            if ("ON".equalsIgnoreCase(action)) power = true;
            else if ("OFF".equalsIgnoreCase(action)) power = false;
            else if ("SET_BRIGHTNESS".equalsIgnoreCase(action) && value != null) brightness = value;
        }

        if (deviceType == DeviceType.AIRCON) {
            if ("ON".equalsIgnoreCase(action)) power = true;
            else if ("OFF".equalsIgnoreCase(action)) power = false;
            else if ("SET_TEMP".equalsIgnoreCase(action) && value != null) targetTemp = value;
        }

        if (deviceType == DeviceType.FAN) {
            if ("ON".equalsIgnoreCase(action)) power = true;
            else if ("OFF".equalsIgnoreCase(action)) power = false;
            // 속도 같은 건 DTO/컬럼 추가 후 확장
        }

        Boolean autoMode = null; // 채팅 제어에서 자동모드까지 만지려면 여기서 세팅

// 지금은 채팅 명령은 자동모드 값을 건드리지 않는 걸 추천
// -> null로 보내면 DeviceStateService가 변경 안 함(영구 유지에 안전)
        DeviceStateUpdateRequest.DevicePatch patch =
                new DeviceStateUpdateRequest.DevicePatch(realDeviceCode, power, brightness, targetTemp, autoMode);

        return new DeviceStateUpdateRequest(List.of(patch));
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

        if (session.getPendingIntent() != null && !session.getPendingIntent().isBlank()) {

            String pendingIntent = session.getPendingIntent();
            Map<String, Object> pendingSlots = readPendingSlots(session);

            Map<String, Object> filled =
                    extractSlotsFromFollowUp(pendingIntent, req.message());

            //  follow-up 단서가 없으면 → pending 해제하고 정상 흐름으로 진행
            if (filled.isEmpty() && !looksLikeFollowUp(req.message())) {
                clearPending(session);
            } else {
                pendingSlots.putAll(filled);

                LlmCommand merged = new LlmCommand(
                        pendingIntent,
                        "",
                        pendingSlots,
                        false,
                        ""
                );

                AtomicReference<ChatResponse> response =
                        new AtomicReference<>(routeByIntent(sessionId, req, merged));

                applyRagFallbackIfNeeded(sessionId, req, response);

                saveMessage(session, Role.ASSISTANT, response.get().answer());
                return response.get();
            }
        }

        // 3. 룰 기반 먼저 시도
        LlmCommand cmd = ruleBasedCommand(sessionId, req.message(), req.memberId());



        // 4. 룰로 못 잡으면 LLM 호출
        if (cmd == null) {
            String cacheKey = makeCacheKey(req.memberId(), req.message());
            LlmCommand cachedCmd = getCached(cacheKey);

            if (cachedCmd != null) {
                cmd = cachedCmd;
            } else {
                String systemPrompt = readSystemPromptCached();
                List<Message> history = buildHistoryMessages(sessionId, systemPrompt);

                String llmRaw = chatClient
                        .prompt()
                        .messages(history)
                        .user(req.message())
                        .call()
                        .content();


                // TTL: 60초
                cmd = parseOrFallback(llmRaw);

                // UNKNOWN + clarification이면 pending용 intent 저장하지 말고 FREE_CHAT로 다운그레이드
                if ("UNKNOWN".equalsIgnoreCase(cmd.intent()) && cmd.needs_clarification()) {
                    cmd = new LlmCommand(
                            "FREE_CHAT",
                            "어떤 정보를 도와드릴까요? (예: 거실 온도 알려줘)",
                            Map.of(),
                            false,
                            ""
                    );
                }

            }
        }
        log.info("[CHAT] sessionId={} memberId={} userMsg='{}' cmd.intent={} needsClar={} replyLen={} slots={}",
                sessionId,
                req.memberId(),
                req.message(),
                (cmd == null ? "null" : cmd.intent()),
                (cmd != null && cmd.needs_clarification()),
                (cmd == null || cmd.reply() == null ? -1 : cmd.reply().length()),
                (cmd == null ? null : cmd.slots())
        );


        // 5. intent 라우팅
        AtomicReference<ChatResponse> response = new AtomicReference<>(routeByIntent(sessionId, req, cmd));
        applyRagFallbackIfNeeded(sessionId, req, response);

        //  RAG fallback: UNKNOWN/FREE_CHAT일 때만 시도
        if ("UNKNOWN".equalsIgnoreCase(response.get().intent()) || "FREE_CHAT".equalsIgnoreCase(response.get().intent())) {
            ragAnswerService.tryAnswer(req.memberId(), req.message(), null)
                    .ifPresent(ragAnswer -> {
                        // intent는 FREE_CHAT으로 두는 게 자연스러움
                        // (RAG는 "답변 생성"이니까)
                        response.set(new ChatResponse(
                                sessionId,
                                ragAnswer,
                                "FREE_CHAT",
                                Map.of("source", "PINECONE_RAG")
                        ));
                    });
        }

        // 6. 어시스턴트 메시지 저장
        saveMessage(session, Role.ASSISTANT, response.get().answer());

        return response.get();
    }
    private boolean looksLikeFollowUp(String message) {
        if (message == null) return false;
        String m = message.trim();

        // "2", "2번"
        if (m.matches("^\\d+$") || m.matches("^\\d+\\s*번$")) return true;

        // 사용자가 흐름 끊고 싶을 때
        if (containsAny(m, "취소", "그만", "닫기", "아니", "됐어")) return true;

        return false;
    }



    private String toMqttCommand(String deviceType, String action, Integer value) {

        if ("LED".equalsIgnoreCase(deviceType)) {
            if ("ON".equalsIgnoreCase(action)) return "LIGHT_ON";
            if ("OFF".equalsIgnoreCase(action)) return "LIGHT_OFF";

            if ("SET_BRIGHTNESS".equalsIgnoreCase(action)) {
                if (value == null) throw new IllegalArgumentException("SET_BRIGHTNESS requires value");
                int v = Math.max(0, Math.min(100, value));
                return "LIGHT_SET_BRIGHTNESS:" + v;
            }
        }
        if ("FAN".equalsIgnoreCase(deviceType)) {
            if ("ON".equalsIgnoreCase(action)) return "FAN_ON";
            if ("OFF".equalsIgnoreCase(action)) return "FAN_OFF";
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
            case "SET_BRIGHTNESS" -> room + " 전등 밝기를 " + value + "%로 설정할게요.";
            default -> "요청을 처리할게요.";
        };
    }

    private void applyRagFallbackIfNeeded(
            String sessionId,
            ChatRequest req,
            AtomicReference<ChatResponse> responseRef
    ) {
        ChatResponse r = responseRef.get();

        boolean needRag =
                (r == null)
                        || (r.answer() == null)
                        || (r.answer().trim().isBlank())
                        || "UNKNOWN".equalsIgnoreCase(r.intent())
                        || "FREE_CHAT".equalsIgnoreCase(r.intent());

        if (!needRag) return;

        ragAnswerService.tryAnswer(req.memberId(), req.message(), null)
                .ifPresent(answer -> responseRef.set(new ChatResponse(
                        sessionId,
                        answer,
                        "RAG",
                        Map.of("source", "pinecone")
                )));
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
            ChatSession session = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));

            // pending 저장 (다음 입력에서 이어가기)
            writePending(session, cmd.intent(), new HashMap<>(cmd.slots()));

            Map<String, Object> data = new HashMap<>(cmd.slots());
            data.put("needs_clarification", true);

            return new ChatResponse(sessionId, cmd.clarify_question(), cmd.intent(), cmd.slots());
        }
        // NOTICE_LIST pending 상태에서 "2번" 같은 입력이 오면 -> NOTICE_DETAIL로 변환
        if ("NOTICE_LIST".equalsIgnoreCase(cmd.intent())) {
            ChatSession s = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));

            if ("NOTICE_LIST".equalsIgnoreCase(s.getPendingIntent())
                    && cmd.slots() != null
                    && cmd.slots().get("index") != null) {

                // intent만 NOTICE_DETAIL로 바꿔서 상세로 보내기
                cmd = new LlmCommand("NOTICE_DETAIL", "", cmd.slots(), false, "");
            }
        }
        // COMPLAINT_LIST pending 상태에서 "1번" 입력 시 -> COMPLAINT_DETAIL
        if ("COMPLAINT_LIST".equalsIgnoreCase(cmd.intent())) {
            ChatSession s = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));

            if ("COMPLAINT_LIST".equalsIgnoreCase(s.getPendingIntent())
                    && cmd.slots() != null
                    && cmd.slots().get("index") != null) {

                cmd = new LlmCommand(
                        "COMPLAINT_DETAIL",
                        "",
                        cmd.slots(),
                        false,
                        ""
                );
            }
        }
        // RESERVATION_LIST pending 상태에서 "1번" 입력 시 -> RESERVATION_DETAIL
        if ("RESERVATION_LIST".equalsIgnoreCase(cmd.intent())) {
            ChatSession s = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));

            if ("RESERVATION_LIST".equalsIgnoreCase(s.getPendingIntent())
                    && cmd.slots() != null
                    && cmd.slots().get("index") != null) {

                cmd = new LlmCommand("RESERVATION_DETAIL", "", cmd.slots(), false, "");
            }
        }



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
            case "SPACE_LIST" -> handleSpaceList(sessionId, req, cmd);
            // 시설 정보
            case "SPACE_INFO" -> handleSpaceInfo(sessionId, req, cmd);
            // 시설 상세
            case "SPACE_BY_CAPACITY" -> handleSpaceByCapacity(sessionId, req, cmd);
            // 공지정보
            case "NOTICE_LIST" -> handleNoticeList(sessionId, req);
            // 상세 공지
            case "NOTICE_DETAIL" -> handleNoticeDetail(sessionId, req, cmd);
            // 민원 정보
            case "COMPLAINT_LIST" -> handleComplaintList(sessionId, req);
            // 민원 상세
            case "COMPLAINT_DETAIL" -> handleComplaintDetail(sessionId, req, cmd);
            // 예약 리스트
            case "RESERVATION_LIST" -> handleReservationList(sessionId, req);
            // 예약 상세 정보
            case "RESERVATION_DETAIL" -> handleReservationDetail(sessionId, req, cmd);

            case "BILL_LIST" -> handleBillList(sessionId, req, cmd);


            case "FREE_CHAT" -> new ChatResponse(
                    sessionId,
                    cmd.reply(),
                    "FREE_CHAT",
                    Map.of()
            );
            // 명령어를 통한 디바이스 제어
            case "DEVICE_CONTROL" -> handleDeviceControl(sessionId, req.memberId(), cmd);

            case "RAG" -> {

                String st = null;
                if (cmd.slots() != null) {
                    Object v = cmd.slots().get("sourceType");
                    if (v != null) st = String.valueOf(v);
                }

                Optional<String> ans = ragAnswerService.tryAnswer(req.memberId(), req.message(), st);

                if (ans.isPresent() && !ans.get().isBlank()) {
                    yield new ChatResponse(
                            sessionId,
                            ans.get(),
                            "RAG",
                            Map.of("source", "PINECONE_RAG", "sourceType", st)
                    );
                }
                yield new ChatResponse(
                        sessionId,
                        "아직 해당 정보가 없어요. (예: '공지사항 보여줘', '헬스장 이용수칙 알려줘')",
                        "FREE_CHAT",
                        Map.of("source", "RAG_EMPTY", "sourceType", st)
                );
            }

            default -> {
                String reply = (cmd.reply() == null) ? "" : cmd.reply().trim();

                log.warn("[CHAT][DEFAULT] sessionId={} intent={} replyLen={} slots={}",
                        sessionId, cmd.intent(), reply.length(), cmd.slots());

                // UNKNOWN인데 reply까지 비었으면 "반드시" 안내문 내려주기
                if (reply.isBlank()) {
                    yield new ChatResponse(
                            sessionId,
                            "어떤 정보를 도와드릴까요? (예: '거실 온도 알려줘', '헬스장 운영시간 알려줘')",
                            "FREE_CHAT",   //
                            Map.of("slots", cmd.slots())
                    );
                }


                // UNKNOWN인데 reply가 있으면 FREE_CHAT로 보정
                if ("UNKNOWN".equalsIgnoreCase(cmd.intent())) {
                    yield new ChatResponse(sessionId, reply, "FREE_CHAT", Map.of());
                }



                yield new ChatResponse(sessionId, reply, cmd.intent(), cmd.slots());
            }


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
        m.setContent(content == null ? "" : content);
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


    private LlmCommand ruleBasedCommand(String sessionId, String message, Long memberId) {


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
        // ---- BILL_LIST 룰 ----
        if (containsAny(m,
                "관리비", "고지서", "청구서", "납부", "납입", "요금",
                "이번달 관리비", "이번 달 관리비", "지난달 관리비", "지난 달 관리비",
                "관리비 내역", "관리비 목록", "고지서 목록"
        )) {
            return new LlmCommand("BILL_LIST", "", Map.of(), false, "");
        }


        // ---- ENV_STATUS 룰 ----
        // 방 이름(필요하면 추가)
        // ---- ENV_STATUS 룰 ----

        if (containsAny(m, "기록", "이력", "추이", "최근")) {
            return null;
        }

        String room = null;
        if (containsAny(m, "거실")) room = "거실";
        else if (containsAny(m, "침실", "안방")) room = "안방";   // 안방을 침실로 매핑(원하면 별도 처리)
        else if (containsAny(m, "부엌", "주방")) room = "주방";
        else if (containsAny(m, "작은방", "오피스방")) room = "작은방";

        // 센서 타입
        String sensorType = null;
        if (containsAny(m, "온도", "temperature")) sensorType = "TEMP";
        else if (containsAny(m, "습도", "humidity")) sensorType = "HUMIDITY";
        else if (containsAny(m, "조도","light")) sensorType = "LIGHT";

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

        // RAG 지식(행사/규칙/FAQ) 룰: 공지보다 먼저!
        if (containsAny(m, "행사", "이벤트", "캠페인", "축제", "기념", "프로그램")) {
            // 기존: EVENT
            return new LlmCommand("RAG", "", Map.of("sourceType", "GUIDE"), false, "");
        }
        if (containsAny(m, "규칙", "수칙", "이용수칙", "룰", "규정", "금지", "안전수칙")) {
            return new LlmCommand("RAG", "", Map.of("sourceType", "RULE"), false, "");
        }
        if (containsAny(m, "faq", "자주", "자주 묻", "문의", "어떻게", "방법", "절차")) {
            return new LlmCommand("RAG", "", Map.of("sourceType", "FAQ"), false, "");
        }

        // =========================
    // DEVICE_CONTROL 룰 (LLM 없이 제어)
    // =========================

        // 방 이름 추출(너가 이미 위에서 room 변수를 만들고 있으니 재사용 가능)
        String ctrlRoom = null;
        if (containsAny(m, "거실")) ctrlRoom = "거실";
        else if (containsAny(m, "침실", "안방")) ctrlRoom = "안방";
        else if (containsAny(m, "작은방", "오피스방")) ctrlRoom = "작은방";

        // 디바이스 타입 추출
        String deviceType = null;
        if (containsAny(m, "전등", "불", "조명", "등")) deviceType = "LED";
        else if (containsAny(m, "에어컨", "선풍기")) deviceType = "FAN";

        // 🔥 (추가) 밝기/어두움 키워드만으로도 LED로 추론 (방이 있을 때만)
        boolean looksBrightness = containsAny(m, "밝기", "밝게", "어둡게", "어둡다", "너무 어둡");
        if (deviceType == null && ctrlRoom != null && looksBrightness) {
            deviceType = "LED";
        }

        // action 추출
        String action = null;
        if (containsAny(m, "켜", "켜줘", "켜 줘", "on", "틀어", "틀어줘")) action = "ON";
        else if (containsAny(m, "꺼", "꺼줘", "꺼 줘", "off", "끄", "꺼줘")) action = "OFF";
        else if (deviceType != null && "LED".equals(deviceType) && looksBrightness) {
            action = "SET_BRIGHTNESS";
        }
        else if (deviceType != null && "AIRCON".equals(deviceType)
                && containsAny(m, "맞춰", "설정", "바꿔", "올려", "내려")) {
            action = "SET_TEMP";
        }

        // 값 추출
        Integer value = null;

        // 에어컨 온도
        if ("AIRCON".equals(deviceType)) {
            Matcher mt = Pattern.compile("(\\d{1,2})\\s*도").matcher(m);
            if (mt.find()) value = Integer.parseInt(mt.group(1));
        }

        // 🔥 LED 밝기 (기존 (\\d{1,3}) 는 너무 광범위해서, "50으로/50%/50퍼센트" 위주로)
        if ("LED".equals(deviceType) && "SET_BRIGHTNESS".equals(action)) {
            Matcher mb1 = Pattern.compile("(\\d{1,3})\\s*(%|퍼센트)").matcher(m);
            if (mb1.find()) value = Integer.parseInt(mb1.group(1));
            else {
                Matcher mb2 = Pattern.compile("(\\d{1,3})\\s*(으로|로)?").matcher(m); // "50으로"
                if (mb2.find()) value = Integer.parseInt(mb2.group(1));
            }
        }



        // DEVICE_CONTROL로 판정 조건
        boolean looksControl = (ctrlRoom != null && deviceType != null && action != null);

        // 온도 설정인데 값이 없으면 되묻게
        if (looksControl && "SET_TEMP".equals(action) && value == null) {
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
            if (value != null) slots.put("value", value);


            return new LlmCommand(
                    "DEVICE_CONTROL",
                    "",
                    slots,
                    false,
                    ""
            );
        }
        // ---- NOTICE 룰 ----
        if (containsAny(m, "공지", "공지사항" )) {
            return new LlmCommand("NOTICE_LIST", "", Map.of(), false, "");
        }

        // ---- COMPLAINT 룰 ----
        if (containsAny(m, "민원", "내 민원", "민원 목록", "민원 확인", "민원 상태")) {
            return new LlmCommand("COMPLAINT_LIST", "", Map.of(), false, "");
        }
        // ---- RESERVATION 룰 ----
        if (containsAny(m, "예약", "내 예약", "예약 내역")) {
            return new LlmCommand("RESERVATION_LIST", "", Map.of(), false, "");
        }

        // ---- FACILITY_INFO 룰 (DB 기반 동적 매칭) ----

        // 시설 관련 키워드가 아예 없으면 패스 (괜히 다 DB조회하지 않게)
        // ※ "가능"은 오탐 많아서 제외 추천
        // =========================
        // SPACE 룰 (가격/정원/인원/룸/타입) - FACILITY_INFO보다 우선!
        // =========================
        boolean looksSpaceQuery =
                containsAny(m, "가격", "비용", "요금", "얼마",
                        "정원", "인원", "몇 명", "수용",
                        "capacity", "룸", "공간", "좌석", "타입", "종류");

        if (looksSpaceQuery) {

            // (1) 내 아파트 시설 목록 조회
            Ho ho = resolveHo(memberId);
            Long apartmentId = resolveApartmentId(ho);
            List<Facility> facilities = facilityRepository.findAllByApartmentId(apartmentId);

            // (2) 현재 메시지에 시설명이 있으면 우선 매칭
            Facility matchedFacility = (facilities == null) ? null : facilities.stream()
                    .filter(f -> matchesFacilityWithAlias(m, f.getName()))
                    .sorted((a, b) -> Integer.compare(b.getName().length(), a.getName().length()))
                    .findFirst()
                    .orElse(null);

            // (3) 버튼처럼 "가격"만 오는 경우(시설명 없음) → 직전 대화에서 시설 추론
            if (matchedFacility == null && sessionId != null && !sessionId.isBlank()) {
                matchedFacility = inferLastFacilityFromSession(sessionId, apartmentId);
            }

            // 시설을 못 찾으면 되묻기
            if (matchedFacility == null) {
                return new LlmCommand(
                        "SPACE_LIST",
                        "확인을 위해 질문할게요.",
                        Map.of(),
                        true,
                        "어느 시설의 가격/공간 정보를 볼까요? (헬스장/스터디룸/골프연습장/게스트하우스/독서실/카페)"
                );
            }

            // (4) 시설에 속한 공간 목록
            List<Space> spaces = spaceRepository.findAllByFacilityId(matchedFacility.getId());
            if (spaces == null || spaces.isEmpty()) {
                // 공간이 없으면 시설 정보로 fallback
                return new LlmCommand(
                        "FACILITY_INFO",
                        "",
                        Map.of("facility", matchedFacility.getName(), "info_type", "DESCRIPTION"),
                        false,
                        ""
                );
            }

            // (5) 메시지에 특정 공간명이 있으면 SPACE_INFO, 아니면 SPACE_LIST
            Space matchedSpace = spaces.stream()
                    .filter(s -> containsNorm(m, s.getName()))
                    .sorted((a, b) -> Integer.compare(b.getName().length(), a.getName().length()))
                    .findFirst()
                    .orElse(null);

            // 인원 숫자 추출(예: "4명", "2인") 있으면 SPACE_BY_CAPACITY로 보낼 수도 있음(선택)
            Integer reqCapacity = null;
            java.util.regex.Matcher capM = java.util.regex.Pattern.compile("(\\d{1,2})\\s*(명|인)").matcher(m);
            if (capM.find()) reqCapacity = Integer.parseInt(capM.group(1));

            if (reqCapacity != null) {
                return new LlmCommand(
                        "SPACE_BY_CAPACITY",
                        "",
                        Map.of("facility", matchedFacility.getName(), "capacity", reqCapacity),
                        false,
                        ""
                );
            }

            // info_type: PRICE/CAPACITY/LIST (SPACE_INFO 내부에서 문장 분기용)
            String infoType = "LIST";
            if (containsAny(m, "가격", "요금", "얼마")) infoType = "PRICE";
            else if (containsAny(m, "정원", "인원", "몇 명", "수용", "capacity")) infoType = "CAPACITY";

            if (matchedSpace != null) {
                return new LlmCommand(
                        "SPACE_INFO",
                        "",
                        Map.of(
                                "facility", matchedFacility.getName(),
                                "space", matchedSpace.getName(),
                                "info_type", infoType
                        ),
                        false,
                        ""
                );
            }
            //  여기까지 왔는데 공간/가격 키워드가 있었으면 UNKNOWN으로 답변하지 말고 차단
            if (containsAny(m, "가격", "요금", "얼마", "정원", "몇 명", "인원", "공간", "방")) {
                return new LlmCommand(
                        "SPACE_LIST",
                        "",
                        Map.of(),
                        true,
                        "어느 시설의 공간/가격 정보를 확인할까요?"
                );
            }


            // 공간명 없이 "가격"만 물으면 → 공간 목록 + 가격 보여주는 쪽(SPACE_LIST)이 자연스러움
            return new LlmCommand(
                    "SPACE_LIST",
                    "",
                    Map.of("facility", matchedFacility.getName(), "info_type", infoType),
                    false,
                    ""
            );
        }



        return null; // 룰로 못 잡으면 LLM로
    }

    /**
     * 버튼 클릭처럼 "가격"만 들어왔을 때,
     * 최근 대화에서 마지막으로 언급된 시설을 추론한다.
     */
    private Facility inferLastFacilityFromSession(String sessionId, Long apartmentId) {
        // 최근 메시지 N개 조회 (너 기존 HISTORY_LIMIT 재사용 가능)
        List<ChatMessage> latest = chatMessageRepository
                .findByChatSession_SessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, 20));

        if (latest == null || latest.isEmpty()) return null;

        List<Facility> facilities = facilityRepository.findAllByApartmentId(apartmentId);
        if (facilities == null || facilities.isEmpty()) return null;

        // 최신 메시지부터 훑으며, 텍스트에 시설명이 포함된 경우를 찾음
        for (ChatMessage msg : latest) {
            String content = msg.getContent();
            if (content == null || content.isBlank()) continue;

            Facility matched = facilities.stream()
                    .filter(f -> matchesFacilityWithAlias(content, f.getName()))
                    .sorted((a, b) -> Integer.compare(b.getName().length(), a.getName().length()))
                    .findFirst()
                    .orElse(null);

            if (matched != null) return matched;
        }
        return null;
    }

    // 시설 별칭 맵 (사용자 표현 → DB 시설명에 포함되는 키워드)
    private static final Map<String, List<String>> FACILITY_ALIASES = Map.of(
            "헬스장", List.of("헬스장", "피트니스", "gym"),
            "스터디룸", List.of("스터디룸", "스터디", "공부방"),
            "실내 골프연습장", List.of("골프장", "골프", "골프연습장"),
            "게스트하우스", List.of("게스트하우스", "게하"),
            "프리미엄 독서실", List.of("독서실", "프리미엄 독서실"),
            "주민 카페", List.of("카페", "주민카페", "커뮤니티 카페")
    );

    private boolean matchesFacilityWithAlias(String message, String facilityName) {
        // 1) 시설명 자체가 포함되면 바로 OK
        if (containsNorm(message, facilityName)) return true;

        // 2) 별칭 매핑 확인
        for (Map.Entry<String, List<String>> entry : FACILITY_ALIASES.entrySet()) {
            String canonical = entry.getKey();

            // DB 시설명이 canonical 을 포함할 때만 alias 검사
            if (!containsNorm(facilityName, canonical)) continue;

            for (String alias : entry.getValue()) {
                if (containsNorm(message, alias)) return true;
            }
        }
        return false;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", "").toLowerCase();
    }

    private static boolean containsNorm(String text, String keyword) {
        String t = norm(text);
        String k = norm(keyword);
        return !k.isBlank() && t.contains(k);
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

    //자신의 아파트 날씨 조회
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


    /* MY_APARTMENT
     * - 내가 속한 아파트의 기본 정보를 조회한다.
     * - memberId → ho → apartmentId 흐름으로 아파트를 식별한다.
     */
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
    /* MY_DONG_HO
     * - 사용자가 현재 거주 중인 동(dong)과 호(ho) 정보를 반환한다.
     * - memberId → resident → ho → dong 관계를 이용한다.
     */
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

    /* DONG_HO_LIST
     * - 특정 동(dong)에 속한 호(ho) 목록을 조회한다.
     * - 기본 동작은 "내가 거주 중인 동" 기준이며,
     *   확장 시 dongId 슬롯을 통해 다른 동도 조회 가능하다.
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
    /* FACILITY_LIST
     * - 내가 속한 아파트의 전체 시설 목록을 조회한다.
     * - 각 시설의 운영시간, 예약 가능 여부 등의 기본 정보를 포함한다.
     */
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
    /* FACILITY_INFO
     * - 특정 시설(Facility)에 대한 상세 정보를 조회한다.
     * - 운영시간 / 예약 가능 여부 / 설명 중 하나를 반환한다.
     * - facility 이름이 없을 경우 최근 대화 기반으로 추론한다.
     */

    private ChatResponse handleFacilityInfo(String sessionId, ChatRequest req, LlmCommand cmd) {
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);

        String facilityName = safeString(cmd.slots().get("facility"));
        String infoType = safeString(cmd.slots().get("info_type")); // HOURS / AVAILABLE / DESCRIPTION

        String message = req.message() == null ? "" : req.message();

        // 1) facilityName 없으면: 최근 세션에서 마지막 시설 추론
        if (facilityName.isBlank() || "UNKNOWN".equalsIgnoreCase(facilityName)) {
            Facility inferred = inferLastFacilityFromSession(sessionId, apartmentId);
            if (inferred != null) {
                facilityName = inferred.getName(); // DB에 있는 진짜 이름으로 고정
            }
        }

        // 2) infoType이 비었으면 message 기반 추정 (기존 네 로직 유지/강화)
        if (infoType.isBlank() || "UNKNOWN".equalsIgnoreCase(infoType)) {
            if (containsAny(message, "운영", "시간", "몇 시", "오픈", "마감")) infoType = "HOURS";
            else if (containsAny(message, "예약", "가능", "예약 가능", "예약돼", "예약 되")) infoType = "AVAILABLE";
            else if (containsAny(message, "설명", "소개", "어디", "위치", "층")) infoType = "DESCRIPTION";
            else {
                // infoType도 못 정하면 되묻기
                return new ChatResponse(
                        sessionId,
                        "운영시간/예약가능/설명 중 어떤 정보를 확인할까요?",
                        "FACILITY_INFO",
                        Map.of("facility", facilityName.isBlank() ? "UNKNOWN" : facilityName, "info_type", "UNKNOWN")
                );
            }
        }

        // 3) 시설 resolve: exact 매치 실패 대비 별칭 매칭으로 안정화
        Facility facility = resolveFacility(apartmentId, facilityName, sessionId);

        if (facility == null) {
            // 절대 예외 던지지 말고 되묻기 (500 방지)
            return new ChatResponse(
                    sessionId,
                    "어느 시설을 확인할까요? (헬스장/스터디룸/골프연습장/게스트하우스/독서실/카페...)",
                    "FACILITY_INFO",
                    Map.of("facility", "UNKNOWN", "info_type", infoType)
            );
        }

        // ===== 여기부터는 기존 네 운영시간/예약가능/설명 분기 로직 그대로 =====
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
                        facility.getName(), start, end, isOpenNow ? "운영 중" : "운영 시간 아님"
                );
            }
            case "AVAILABLE" -> {
                answer = String.format(
                        "%s은(는) %s. %s",
                        facility.getName(),
                        reservableNow ? "현재 예약 가능합니다" : "현재 예약이 불가능합니다",
                        reservationAvailable ? "" : "예약 기능이 제공되지 않는 시설입니다"
                ).trim();
            }
            case "DESCRIPTION" -> {
                String desc = safeString(facility.getDescription());
                if (desc.isBlank()) desc = "등록된 설명이 없습니다.";
                answer = String.format("%s 설명: %s", facility.getName(), desc);
                data.put("description", desc);
            }
            default -> {
                answer = "운영시간/예약가능/설명 중 어떤 정보를 확인할까요?";
            }
        }

        return new ChatResponse(sessionId, answer, "FACILITY_INFO", data);
    }

    private Facility resolveFacility(Long apartmentId, String facilityName, String sessionId) {
        if (facilityName == null || facilityName.isBlank() || "UNKNOWN".equalsIgnoreCase(facilityName)) {
            return inferLastFacilityFromSession(sessionId, apartmentId);
        }

        // 1) exact
        Optional<Facility> exact = facilityRepository.findByApartmentIdAndName(apartmentId, facilityName);
        if (exact.isPresent()) return exact.get();

        // 2) alias match (골프장 -> 실내 골프연습장 같은 케이스)
        List<Facility> facilities = facilityRepository.findAllByApartmentId(apartmentId);
        if (facilities == null || facilities.isEmpty()) return null;

        return facilities.stream()
                .filter(f -> matchesFacilityWithAlias(facilityName, f.getName()) || matchesFacilityWithAlias(sessionId == null ? "" : facilityName, f.getName()))
                .sorted((a, b) -> Integer.compare(b.getName().length(), a.getName().length()))
                .findFirst()
                .orElse(null);
    }

    /* ENV_HISTORY
     * - 특정 방(room)의 환경 변화 이력을 조회한다.
     * - 최근 N개의 센서 기록을 시간 역순으로 반환한다.
     */

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
            case "HUMIDITY", "HUMID" -> "습도";
            case "LIGHT" -> "조도";
            default -> "환경";
        };
    }


    /* ENV_STATUS
     * - 특정 방(room)의 현재 환경 상태를 조회한다.
     * - 온도/습도/조도 등의 최신 센서 값을 반환한다.
     */
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
        Optional<Object> roomOpt =
                roomRepository.findByHo_IdAndName(ho.getId(), roomName);

        if (roomOpt.isEmpty()) {
            return new ChatResponse(
                    sessionId,
                    "해당 방을 찾을 수 없습니다. 등록된 방을 확인해보세요.",
                    "DEVICE_CONTROL",
                    Map.of()
            );
        }
        Room room = (Room) roomOpt.get();


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
    /* ROOM_LIST
     * - 사용자의 세대(ho)에 등록된 방 목록을 조회한다.
     * - 방 이름 위주의 간단한 리스트를 반환한다.
     */

    private ChatResponse handleRoomList(String sessionId, ChatRequest req) {
        Ho ho = resolveHo(req.memberId());

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
    /* SPACE_LIST
     * - 특정 시설에 속한 공간(Space) 목록을 조회한다.
     * - 가격/정원/공간 목록 등 목적에 따라 요약 형태로 응답한다.
     */
    private ChatResponse handleSpaceList(String sessionId, ChatRequest req, LlmCommand cmd) {
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);

        String facilityName = safeString(cmd.slots().get("facility"));
        String infoType = safeString(cmd.slots().get("info_type")); // PRICE / CAPACITY / LIST

        if (facilityName.isBlank() || "UNKNOWN".equalsIgnoreCase(facilityName)) {
            return new ChatResponse(sessionId, "어느 시설의 공간(룸/좌석)을 볼까요?", "SPACE_LIST", Map.of());
        }

        Facility facility = resolveFacility(apartmentId, facilityName, sessionId);
        if (facility == null) {
            return new ChatResponse(
                    sessionId,
                    "시설을 찾을 수 없습니다. 어느 시설의 공간 정보를 볼까요?",
                    "SPACE_LIST",
                    Map.of("facility", "UNKNOWN")
            );
        }


        List<Space> spaces = spaceRepository.findAllByFacilityId(facility.getId());

        if (spaces.isEmpty()) {
            return new ChatResponse(sessionId, facilityName + "에 등록된 공간 정보가 없습니다.", "SPACE_LIST", Map.of());
        }

        List<Map<String, Object>> payload = spaces.stream()
                .map(s -> Map.<String, Object>of(
                        "spaceId", s.getId(),
                        "name", s.getName(),
                        "minCapacity", s.getMinCapacity(),
                        "maxCapacity", s.getMaxCapacity(),
                        "price", s.getPrice()
                ))
                .toList();

        //  답변 문장을 infoType에 맞게 구성 (프론트가 answer만 렌더링해도 가격 보이게)
        String answer;
        if ("PRICE".equalsIgnoreCase(infoType)) {
            // 가격 요약: "A 20000원, B 50000원 ..."
            String priceSummary = spaces.stream()
                    .map(s -> s.getName() + " " + s.getPrice() + "원")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            answer = facilityName + " 가격은 " + priceSummary + " 입니다.";
        } else if ("CAPACITY".equalsIgnoreCase(infoType)) {
            String capSummary = spaces.stream()
                    .map(s -> s.getName() + " (" + s.getMinCapacity() + "~" + s.getMaxCapacity() + "명)")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            answer = facilityName + " 정원 정보는 " + capSummary + " 입니다.";
        } else {
            String names = spaces.stream().map(Space::getName).distinct().reduce((a, b) -> a + ", " + b).orElse("");
            answer = facilityName + " 공간은 " + names + " 입니다.";
        }

        return new ChatResponse(
                sessionId,
                answer,
                "SPACE_LIST",
                Map.of(
                        "facilityId", facility.getId(),
                        "facility", facilityName,
                        "info_type", infoType.isBlank() ? "LIST" : infoType,
                        "spaces", payload
                )
        );
    }
    /* SPACE_INFO
     * - 시설(Facility) 내부의 특정 공간(Space)에 대한 상세 정보를 조회한다.
     * - 공간의 가격, 수용 인원(min/max capacity) 정보를 제공한다.
     */

    private ChatResponse handleSpaceInfo(String sessionId, ChatRequest req, LlmCommand cmd) {

        // 1) 로그인 사용자 기준 아파트 식별
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);

        // 2) 슬롯에서 facility / space 이름 추출
        String facilityName = safeString(cmd.slots().get("facility"));
        String spaceName = safeString(cmd.slots().get("space"));

        // 시설명이 없으면 되묻기
        if (facilityName.isBlank()) {
            return new ChatResponse(
                    sessionId,
                    "어느 시설의 공간을 확인할까요?",
                    "SPACE_INFO",
                    Map.of()
            );
        }

        // 공간명이 없으면 목록 유도
        if (spaceName.isBlank()) {
            return new ChatResponse(
                    sessionId,
                    facilityName + "에는 여러 공간이 있어요. 어떤 공간을 확인할까요?",
                    "SPACE_INFO",
                    Map.of("facility", facilityName)
            );
        }

        // 3) 시설 조회 (아파트 범위 한정)
        Facility facility = resolveFacility(apartmentId, facilityName, sessionId);
        if (facility == null) {
            return new ChatResponse(
                    sessionId,
                    "시설을 찾을 수 없습니다. 어느 시설의 공간 정보를 볼까요?",
                    "SPACE_LIST",
                    Map.of("facility", "UNKNOWN")
            );
        }


        // 4) 시설 + 공간명으로 Space 조회
        Space space = (Space) spaceRepository.findByFacility_IdAndName(
                facility.getId(),
                spaceName
        ).orElseThrow(() ->
                new IllegalArgumentException("공간을 찾을 수 없습니다: " + spaceName));

        // 5) 응답 데이터 구성
        Map<String, Object> data = new HashMap<>();
        data.put("facility", facilityName);
        data.put("space", space.getName());
        data.put("minCapacity", space.getMinCapacity());
        data.put("maxCapacity", space.getMaxCapacity());
        data.put("price", space.getPrice());

        // 6) 사용자 응답 문장 생성
        String answer = String.format(
                "%s의 %s는 최대 %d명까지 이용 가능하며, 가격은 %d원입니다.",
                facilityName,
                space.getName(),
                space.getMaxCapacity(),
                space.getPrice()
        );

        return new ChatResponse(
                sessionId,
                answer,
                "SPACE_INFO",
                data
        );
    }


    private Map<String, Object> readPendingSlots(ChatSession session) {
        if (session.getPendingSlotsJson() == null || session.getPendingSlotsJson().isBlank())
            return new HashMap<>();
        try {
            return objectMapper.readValue(session.getPendingSlotsJson(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void writePending(ChatSession session, String intent, Map<String, Object> slots) {
        try {
            session.setPendingIntent(intent);
            session.setPendingSlotsJson(objectMapper.writeValueAsString(slots));
            chatSessionRepository.save(session);
        } catch (Exception ignored) {}
    }

    private void clearPending(ChatSession session) {
        session.setPendingIntent(null);
        session.setPendingSlotsJson(null);
        chatSessionRepository.save(session);
    }

    private Map<String, Object> extractSlotsFromFollowUp(String intent, String message) {
        Map<String, Object> slots = new HashMap<>();
        if (message == null) return slots;

        // FOLLOW-UP에서는 "추측" 금지. 키워드 매칭만.
        switch (intent) {
            case "FACILITY_INFO" -> {
                if (containsAny(message, "운영", "시간", "몇 시",  "오픈", "마감")) slots.put("info_type", "HOURS");
                else if (containsAny(message, "예약", "가능")) slots.put("info_type", "AVAILABLE");
                else if (containsAny(message, "설명", "소개", "어디", "위치", "층")) slots.put("info_type", "DESCRIPTION");
            }
            case "DEVICE_CONTROL" -> {
                // 1) 온도: "24도"
                Matcher mt = Pattern.compile("(\\d{1,2})\\s*도").matcher(message);
                if (mt.find()) {
                    slots.put("value", Integer.parseInt(mt.group(1)));
                    break;
                }

                // 2) 밝기: "17%", "17 퍼센트"
                Matcher mb = Pattern.compile("(\\d{1,3})\\s*(%|퍼센트)").matcher(message);
                if (mb.find()) {
                    slots.put("value", Integer.parseInt(mb.group(1)));
                }
            }

            case "ENV_STATUS", "ENV_HISTORY" -> {
                if (containsAny(message, "온도")) slots.put("sensor_type", "TEMP");
                else if (containsAny(message, "습도")) slots.put("sensor_type", "HUMIDITY");
                else if (containsAny(message, "조도")) slots.put("sensor_type", "LIGHT");

                if (containsAny(message, "거실")) slots.put("room", "거실");
                else if (containsAny(message, "침실", "안방")) slots.put("room", "안방");
                else if (containsAny(message, "주방", "부엌")) slots.put("room", "주방");
                else if (containsAny(message, "작은방", "오피스방")) slots.put("room", "작은방");
            }

            case "NOTICE_LIST" -> {
                // 1) "2번", "2 번"
                Matcher mi = Pattern.compile("(\\d+)\\s*번").matcher(message);
                if (mi.find()) {
                    slots.put("index", Integer.parseInt(mi.group(1)));
                    break;
                }

                // 2) 버튼 클릭 등: "2"
                Matcher mi2 = Pattern.compile("^\\s*(\\d+)\\s*$").matcher(message);
                if (mi2.find()) {
                    slots.put("index", Integer.parseInt(mi2.group(1)));
                }
            }
            case "COMPLAINT_LIST" -> {
                // 1) "2번", "2 번"
                Matcher mi = Pattern.compile("(\\d+)\\s*번").matcher(message);
                if (mi.find()) {
                    slots.put("index", Integer.parseInt(mi.group(1)));
                    break;
                }
                // 2) 버튼 클릭 등: "2"
                Matcher mi2 = Pattern.compile("^\\s*(\\d+)\\s*$").matcher(message);
                if (mi2.find()) {
                    slots.put("index", Integer.parseInt(mi2.group(1)));
                }
            }
            case "RESERVATION_LIST" -> {
                Matcher mi = Pattern.compile("(\\d+)\\s*번").matcher(message);
                if (mi.find()) {
                    slots.put("index", Integer.parseInt(mi.group(1)));
                    break;
                }

                Matcher mi2 = Pattern.compile("^\\s*(\\d+)\\s*$").matcher(message);
                if (mi2.find()) {
                    slots.put("index", Integer.parseInt(mi2.group(1)));
                }
            }




        }
        return slots;
    }
    /* SPACE_BY_CAPACITY
     * - 지정한 인원(capacity)을 수용할 수 있는 공간(Space) 목록을 조회한다.
     * - 시설명 + 인원 수를 기준으로 필터링한다.
     */
    private ChatResponse handleSpaceByCapacity(String sessionId, ChatRequest req, LlmCommand cmd) {
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);

        String facilityName = safeString(cmd.slots().get("facility"));
        Integer capacity = (cmd.slots().get("capacity") instanceof Number n) ? n.intValue() : null;

        if (facilityName.isBlank()) {
            return new ChatResponse(sessionId, "어느 시설에서 인원에 맞는 공간을 찾을까요?", "SPACE_BY_CAPACITY", Map.of());
        }
        if (capacity == null) {
            return new ChatResponse(sessionId, "몇 명 이용할 예정인가요? (예: 4명)", "SPACE_BY_CAPACITY",
                    Map.of("facility", facilityName));
        }

        Facility facility = resolveFacility(apartmentId, facilityName, sessionId);
        if (facility == null) {
            return new ChatResponse(
                    sessionId,
                    "시설을 찾을 수 없습니다. 어느 시설에서 인원에 맞는 공간을 찾을까요?",
                    "SPACE_BY_CAPACITY",
                    Map.of("facility", "UNKNOWN", "capacity", capacity)
            );
        }



        List<Space> spaces = spaceRepository.findSpacesByCapacity(facility.getId(), capacity);

        if (spaces == null || spaces.isEmpty()) {
            return new ChatResponse(
                    sessionId,
                    facilityName + "에서 " + capacity + "명 이용 가능한 공간이 없습니다.",
                    "SPACE_BY_CAPACITY",
                    Map.of("facility", facilityName, "capacity", capacity, "spaces", List.of())
            );
        }

        List<Map<String, Object>> payload = spaces.stream()
                .map(s -> Map.<String, Object>of(
                        "spaceId", s.getId(),
                        "name", s.getName(),
                        "minCapacity", s.getMinCapacity(),
                        "maxCapacity", s.getMaxCapacity(),
                        "price", s.getPrice()
                ))
                .toList();

        String names = spaces.stream().map(Space::getName).distinct().reduce((a, b) -> a + ", " + b).orElse("");

        return new ChatResponse(
                sessionId,
                facilityName + "에서 " + capacity + "명 이용 가능한 공간은 " + names + " 입니다.",
                "SPACE_BY_CAPACITY",
                Map.of(
                        "facilityId", facility.getId(),
                        "facility", facilityName,
                        "capacity", capacity,
                        "spaces", payload
                )
        );

    }

    private ChatResponse handleBillList(String sessionId, ChatRequest req, LlmCommand cmd) {
        Long memberId = req.memberId();
        if (memberId == null) {
            return new ChatResponse(sessionId, "로그인이 필요해요.", "BILL_LIST", Map.of("bills", List.of()));
        }

        // memberId -> hoId (예: memberRepository로 member 조회 후 resident.ho.id)
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));
        Long hoId = member.getResident().getHo().getId();

        // 페이징 0페이지, 10개

        Page<BillSummaryResponse> page = billService.getBillsByHo(hoId, PageRequest.of(0, 10));

        List<Map<String, Object>> payload = new ArrayList<>();
        int idx = 1;
        for (BillSummaryResponse b : page.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("index", idx++);
            m.put("billId", b.getBillId());        // DTO 필드명에 맞춰 수정
            m.put("billMonth", b.getBillMonth());
            m.put("totalPrice", b.getTotalPrice());
            m.put("status", String.valueOf(b.getStatus()));
            m.put("dueDate", b.getDueDate());
            payload.add(m);
        }

        if (payload.isEmpty()) {
            return new ChatResponse(sessionId, "조회된 관리비 고지서가 없어요.", "BILL_LIST", Map.of("bills", List.of()));
        }

        String listText = payload.stream()
                .map(p -> p.get("index") + "번) " + p.get("billMonth") + " 관리비 (" + p.get("totalPrice") + "원)")
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String answer = "관리비 고지서 목록입니다.\n" + listText + "\n\n자세히 볼 번호를 말해줘요. (예: 1번)";

        // pending 저장 (상세에서 index -> billId 찾기)
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));

        Map<String, Object> pending = new HashMap<>();
        pending.put("bills", payload);
        writePending(session, "BILL_LIST", pending);

        return new ChatResponse(sessionId, answer, "BILL_LIST", Map.of("bills", payload));
    }


    /* NOTICE_LIST
     * - 단지 전체 공지 + 내 동 대상 공지를 최신순으로 조회한다.
     * - 목록 응답 후 번호(index) 입력을 통해 상세 조회로 이어진다.
     */
    private ChatResponse handleNoticeList(String sessionId, ChatRequest req) {
        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);
        Long dongId = ho.getDong().getId();

        // 단지(ALL) + 내동(DONG) 공지 최신순 조회
        List<Notice> notices = noticeRepository.findBoardNotices(apartmentId, dongId);

        if (notices == null || notices.isEmpty()) {
            return new ChatResponse(sessionId, "현재 확인할 공지사항이 없습니다.", "NOTICE_LIST", Map.of("notices", List.of()));
        }

        // UI용: 10개만 보여주자(원하면 늘려)
        int limit = Math.min(10, notices.size());

        List<Map<String, Object>> payload = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Notice n = notices.get(i);
            payload.add(Map.<String, Object>of(
                    "index", i + 1,
                    "noticeId", n.getId(),
                    "title", safeString(n.getTitle()),
                    "createdAt", n.getCreatedAt(),
                    "targetScope", String.valueOf(n.getTargetScope())
            ));
        }

        String listText = payload.stream()
                .map(p -> p.get("index") + "번) " + p.get("title"))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String answer = "공지사항 목록입니다.\n" + listText + "\n\n자세히 볼 번호를 말해줘요. (예: 2번)";

        // pending 저장: 다음 입력에서 index로 noticeId 찾아 상세로
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));

        Map<String, Object> pending = new HashMap<>();
        pending.put("notices", payload); // index -> noticeId 매핑
        writePending(session, "NOTICE_LIST", pending);

        return new ChatResponse(sessionId, answer, "NOTICE_LIST", Map.of("notices", payload));
    }
    /* NOTICE_DETAIL
     * - 공지사항 목록에서 선택한 공지의 상세 내용을 조회한다.
     * - 동 대상 공지의 경우, 사용자의 동 포함 여부를 검증한다.
     */
    private ChatResponse handleNoticeDetail(String sessionId, ChatRequest req, LlmCommand cmd) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));

        Ho ho = resolveHo(req.memberId());
        Long apartmentId = resolveApartmentId(ho);
        Long myDongId = ho.getDong().getId();

        // 1) index -> noticeId 매핑 (pending.notices에서 찾음)
        Long noticeId = null;

        Object idxObj = cmd.slots().get("index");
        if (idxObj instanceof Number n) {
            int index = n.intValue();

            Map<String, Object> pending = readPendingSlots(session);
            Object noticesObj = pending.get("notices");

            if (noticesObj instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> m) {
                        Object i = m.get("index");
                        Object nid = m.get("noticeId");
                        if (i instanceof Number in && in.intValue() == index && nid instanceof Number nn) {
                            noticeId = nn.longValue();
                            break;
                        }
                    }
                }
            }
        }

        if (noticeId == null) {
            // 번호를 못 알아먹으면 다시 질문 (pending 유지)
            writePending(session, "NOTICE_LIST", readPendingSlots(session));
            return new ChatResponse(sessionId, "몇 번 공지를 볼까요? (예: 2번)", "NOTICE_DETAIL", Map.of());
        }

        // 2) 공지 조회
        Long finalNoticeId = noticeId;
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지 없음: " + finalNoticeId));

//        // 3) 보안 체크: 같은 단지인지
//        Long noticeApartmentId = notice.getAdmin().getApartment().getId();
//        if (!noticeApartmentId.equals(apartmentId)) {
//            throw new IllegalArgumentException("접근 권한이 없습니다.");
//        }


        // 4) DONG 공지면 내 동 포함인지 체크
        if (notice.getTargetScope() == NoticeTargetScope.DONG) {
            List<Long> dongIds = noticeTargetDongRepository.findDongIdsByNoticeId(noticeId);
            if (dongIds == null || !dongIds.contains(myDongId)) {
                throw new IllegalArgumentException("접근 권한이 없습니다.");
            }
        }

        // 5) 상세 열었으면 pending 제거(1차에서는 읽음 처리 안함)
       // clearPending(session);

        String answer = "📌 " + safeString(notice.getTitle()) + "\n\n" + safeString(notice.getContent());

        return new ChatResponse(
                sessionId,
                answer,
                "NOTICE_DETAIL",
                Map.of(
                        "noticeId", notice.getId(),
                        "title", safeString(notice.getTitle()),
                        "content", safeString(notice.getContent()),
                        "createdAt", notice.getCreatedAt(),
                        "targetScope", String.valueOf(notice.getTargetScope())
                )
        );
    }
    /* COMPLAINT_LIST
     * - 로그인한 사용자가 등록한 민원 목록을 조회한다.
     * - 최신순으로 정렬되며, 번호 선택을 통해 상세 조회로 이어진다.
     */

    private ChatResponse handleComplaintList(String sessionId, ChatRequest req) {
        Long memberId = req.memberId();

        List<Complaint> complaints = complaintRepository.findByMember_IdAndDeletedFalse(memberId);

        if (complaints == null || complaints.isEmpty()) {
            return new ChatResponse(sessionId, "등록된 민원이 없습니다.", "COMPLAINT_LIST", Map.of("complaints", List.of()));
        }

        // 최신순 정렬(레포가 order by가 없어서 안전하게)
        complaints.sort(
                Comparator.comparing(
                        Complaint::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed()
        );

        int limit = Math.min(10, complaints.size());

        List<Map<String, Object>> payload = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            Complaint c = complaints.get(i);
            payload.add(Map.<String, Object>of(
                    "index", i + 1,
                    "complaintId", c.getId(),
                    "title", safeString(c.getTitle()),
                    "status", String.valueOf(c.getStatus()),
                    "type", String.valueOf(c.getType()),
                    "createdAt", c.getCreatedAt()
            ));
        }

        String listText = payload.stream()
                .map(p -> p.get("index") + "번) " + p.get("title") + " (" + p.get("status") + ")")
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        String answer = "내 민원 목록입니다.\n" + listText + "\n\n자세히 볼 번호를 말해줘요. (예: 2번)";

        // pending 저장
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));

        Map<String, Object> pending = new HashMap<>();
        pending.put("complaints", payload); // index -> complaintId 매핑
        writePending(session, "COMPLAINT_LIST", pending);

        return new ChatResponse(sessionId, answer, "COMPLAINT_LIST", Map.of("complaints", payload));
    }
    /* COMPLAINT_DETAIL
     * - 선택한 민원의 상세 내용을 조회한다.
     * - 민원 내용 + 상태 + 관리자 답변(있을 경우)을 함께 반환한다.
     * - memberId 기준으로 소유자 검증을 수행한다.
     */
    private ChatResponse handleComplaintDetail(String sessionId, ChatRequest req, LlmCommand cmd) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션 없음: " + sessionId));

        Long memberId = req.memberId();

        // 1) index -> complaintId
        Long complaintId = null;
        Object idxObj = cmd.slots().get("index");

        if (idxObj instanceof Number n) {
            int index = n.intValue();

            Map<String, Object> pending = readPendingSlots(session);
            Object listObj = pending.get("complaints");

            if (listObj instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> m) {
                        Object i = m.get("index");
                        Object cid = m.get("complaintId");
                        if (i instanceof Number in && in.intValue() == index && cid instanceof Number nn) {
                            complaintId = nn.longValue();
                            break;
                        }
                    }
                }
            }
        }


        if (complaintId == null) {
            // 번호 못 읽으면 다시 질문 (pending 유지)
            writePending(session, "COMPLAINT_LIST", readPendingSlots(session));
            return new ChatResponse(sessionId, "몇 번 민원을 볼까요? (예: 2번)", "COMPLAINT_LIST", Map.of());
        }

        // 2) 민원 조회 + 소유자 검증(추천 메소드 사용)
        Complaint complaint = complaintRepository.findByIdAndMember_IdAndDeletedFalse(complaintId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("민원을 찾을 수 없거나 접근 권한이 없습니다."));

        // 3) 답변 조회(있으면 최신 1개만)
        List<ComplaintAnswer> answers = complaintAnswerRepository
                .findByComplaint_IdOrderByCreatedAtAsc(complaintId);

        ComplaintAnswer last = (answers == null || answers.isEmpty()) ? null : answers.get(answers.size() - 1);

        // 4) pending clear
        clearPending(session);

        String answerText =
                "📌 " + safeString(complaint.getTitle()) + "\n"
                        + "상태: " + String.valueOf(complaint.getStatus()) + "\n\n"
                        + safeString(complaint.getContent());

        if (last != null) {
            answerText += "\n\n📝 답변\n" + safeString(last.getResultContent());
        } else {
            answerText += "\n\n📝 답변\n아직 답변이 등록되지 않았습니다.";
        }

        Map<String, Object> data = new HashMap<>();
        data.put("complaintId", complaint.getId());
        data.put("title", safeString(complaint.getTitle()));
        data.put("content", safeString(complaint.getContent()));
        data.put("status", String.valueOf(complaint.getStatus()));
        data.put("type", String.valueOf(complaint.getType()));
        data.put("createdAt", complaint.getCreatedAt());
        if (last != null) {
            data.put("answer", Map.of(
                    "answerId", last.getId(),
                    "resultContent", safeString(last.getResultContent()),
                    "createdAt", last.getCreatedAt()
            ));
        }

        return new ChatResponse(sessionId, answerText, "COMPLAINT_DETAIL", data);
    }

    /* RESERVATION_LIST
     * - 로그인한 사용자의 시설 예약 목록을 조회한다.
     * - 예약 공간, 시간, 상태 정보를 포함한다.
     */
        private ChatResponse handleReservationList(String sessionId, ChatRequest req) {

        List<ReservationResponse> reservations =
                reservationService.getMyReservations(req.memberId());

        if (reservations.isEmpty()) {
            return new ChatResponse(
                    sessionId,
                    "현재 예약 내역이 없습니다.",
                    "RESERVATION_LIST",
                    Map.of()
            );
        }

        List<Map<String, Object>> payload = new ArrayList<>();
        for (int i = 0; i < reservations.size(); i++) {
            ReservationResponse r = reservations.get(i);
            payload.add(Map.of(
                    "index", i + 1,
                    "reservationId", r.id(),
                    "spaceName", r.spaceName(),
                    "startTime", r.startTime(),
                    "status", r.status()
            ));
        }

        String listText = payload.stream()
                .map(p -> p.get("index") + "번) "
                        + p.get("spaceName") + " / "
                        + p.get("startTime"))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow();
        writePending(session, "RESERVATION_LIST", Map.of("reservations", payload));

        return new ChatResponse(
                sessionId,
                "내 예약 목록입니다.\n" + listText + "\n\n자세히 볼 번호를 말해줘요.",
                "RESERVATION_LIST",
                Map.of("reservations", payload)
        );
    }
    /* RESERVATION_DETAIL
     * - 선택한 예약의 상세 정보를 조회한다.
     * - 이용 시간, 인원, 금액, 상태 정보를 반환한다.
     */
    private ChatResponse handleReservationDetail(String sessionId, ChatRequest req, LlmCommand cmd) {

        ChatSession session = chatSessionRepository.findById(sessionId).orElseThrow();

        Integer index = (cmd.slots().get("index") instanceof Number n) ? n.intValue() : null;
        if (index == null) {
            return new ChatResponse(sessionId, "몇 번 예약을 볼까요? (예: 1번)", "RESERVATION_DETAIL", Map.of());
        }

        Map<String, Object> pending = readPendingSlots(session);
        Object listObj = pending.get("reservations");

        if (!(listObj instanceof List<?> rawList) || rawList.isEmpty()) {
            // pending이 없거나 만료/다른 intent로 덮였을 때
            return new ChatResponse(
                    sessionId,
                    "예약 목록이 없거나 만료됐어. 먼저 '내 예약 목록 보여줘'를 다시 입력해줘.",
                    "RESERVATION_LIST",
                    Map.of()
            );
        }

        int i = index - 1;
        if (i < 0 || i >= rawList.size()) {
            return new ChatResponse(
                    sessionId,
                    "번호가 범위를 벗어났어. 1부터 " + rawList.size() + " 중에서 골라줘.",
                    "RESERVATION_DETAIL",
                    Map.of()
            );
        }

        Object rowObj = rawList.get(i);
        if (!(rowObj instanceof Map<?, ?> row)) {
            return new ChatResponse(sessionId, "예약 정보를 읽을 수 없어요. 다시 '내 예약 목록 보여줘'를 입력해줘.", "RESERVATION_LIST", Map.of());
        }

        Object ridObj = row.get("reservationId");
        Long reservationId = (ridObj instanceof Number nn) ? nn.longValue() : null;
        if (reservationId == null) {
            return new ChatResponse(sessionId, "예약 ID를 찾을 수 없어요. 다시 '내 예약 목록 보여줘'를 입력해줘.", "RESERVATION_LIST", Map.of());
        }

        ReservationResponse r = reservationService.getReservationDetails(req.memberId(), reservationId);

        clearPending(session);

        String answer = """
        📌 %s
        시간: %s ~ %s
        인원: %d명
        금액: %d원
        상태: %s
        """.formatted(
                r.spaceName(),
                r.startTime(),
                r.endTime(),
                r.capacity(),
                r.totalPrice(),
                r.status()
        );

        return new ChatResponse(sessionId, answer, "RESERVATION_DETAIL", Map.of("reservation", r));
    }

    private String normalizeDeviceCode(String deviceType) {
        String t = safeString(deviceType).toUpperCase();
        if (t.isBlank()) return "";
        if (t.equals("LED") || t.equals("AIRCON") || t.equals("FAN")) return t;

        if (t.contains("전등") || t.contains("조명") || t.contains("등") || t.contains("불")) return "LED";
        if (t.contains("에어컨")) return "AIRCON";
        if (t.contains("에어컨") || t.contains("에어컨")) return "FAN";

        return t;
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

    private LlmCommand parseOrFallback(String llmRaw) {
        try {
            String cleaned = cleanJson(llmRaw);
            LlmCommand cmd = objectMapper.readValue(cleaned, LlmCommand.class);

            //  UNKNOWN인데 reply가 길면(=자연어 답변) 강제로 FREE_CHAT로 보정
            if ("UNKNOWN".equalsIgnoreCase(cmd.intent())
                    && cmd.reply() != null
                    && !cmd.reply().isBlank()
                    && !cmd.needs_clarification()) {
                return new LlmCommand("FREE_CHAT", cmd.reply(), Map.of(), false, "");
            }

            return cmd;
        } catch (Exception e) {
            return new LlmCommand(
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
