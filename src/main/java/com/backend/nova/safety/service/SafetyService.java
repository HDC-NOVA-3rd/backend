package com.backend.nova.safety.service;

import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.facility.entity.Facility;
import com.backend.nova.facility.repository.FacilityRepository;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.global.notification.NotificationService;
import com.backend.nova.global.notification.PushMessageRequest;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.safety.dto.SafetySensorInboundPayload;
import com.backend.nova.safety.dto.SafetyEventLogResponse;
import com.backend.nova.safety.dto.SafetyMqttUpdatePayload;
import com.backend.nova.safety.dto.SafetyLockRequest;
import com.backend.nova.safety.dto.SafetyLockResponse;
import com.backend.nova.safety.dto.SafetySensorLogResponse;
import com.backend.nova.safety.dto.SafetyStatusResponse;
import com.backend.nova.safety.entity.SafetyEventLog;
import com.backend.nova.safety.entity.SafetySensor;
import com.backend.nova.safety.entity.SafetySensorLog;
import com.backend.nova.safety.entity.SafetyStatusEntity;
import com.backend.nova.safety.enums.SafetyReason;
import com.backend.nova.safety.enums.SafetyStatus;
import com.backend.nova.safety.enums.SensorType;
import com.backend.nova.safety.repository.SafetyEventLogRepository;
import com.backend.nova.safety.repository.SafetyStatusRepository;
import com.backend.nova.safety.repository.SensorLogRepository;
import com.backend.nova.safety.repository.SensorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SafetyService {
    private static final String REQUEST_FROM_MQTT = "mqtt";
    private static final double HEAT_DANGER_THRESHOLD = 70.0;
    private static final double GAS_DANGER_THRESHOLD = 500.0;

    private final FacilityRepository facilityRepository;
    private final DongRepository dongRepository;
    private final SafetyEventLogRepository safetyEventLogRepository;
    private final SafetyStatusRepository safetyStatusRepository;
    private final SensorLogRepository sensorLogRepository;
    private final SensorRepository sensorRepository;
    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    public List<SafetyStatusResponse> listSafetyStatus(Long apartmentId) {
        if (apartmentId == null || apartmentId <= 0) {
            return List.of();
        }
        List<SafetyStatusEntity> statusEntityList = safetyStatusRepository.findByApartmentIdOrderByUpdatedAtDesc(apartmentId);
        Set<Long> facilityIdSet = statusEntityList.stream()
                .map(SafetyStatusEntity::getFacilityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> dongIdSet = statusEntityList.stream()
                .map(SafetyStatusEntity::getDongId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> facilityNameById = facilityRepository.findAllById(facilityIdSet).stream()
                .filter(facility -> facility.getApartment().getId().equals(apartmentId))
                .collect(Collectors.toMap(Facility::getId, Facility::getName));
        Map<Long, String> dongNoById = dongRepository.findAllById(dongIdSet).stream()
                .filter(dong -> dong.getApartment().getId().equals(apartmentId))
                .collect(Collectors.toMap(Dong::getId, Dong::getDongNo));

        return statusEntityList.stream()
                .map(entity -> {
                    String dongNo = entity.getDongId() == null ? null : dongNoById.get(entity.getDongId());
                    String facilityName = entity.getFacilityId() == null ? null : facilityNameById.get(entity.getFacilityId());
                    return new SafetyStatusResponse(
                            dongNo,
                            facilityName,
                            entity.getSafetyStatus(),
                            entity.getReason(),
                            entity.getUpdatedAt()
                    );
                })
                .toList();
    }

    public List<SafetyEventLogResponse> listSafetyEventLogs(Long apartmentId) {
        if (apartmentId == null || apartmentId <= 0) {
            return List.of();
        }
        List<SafetyEventLog> logs = safetyEventLogRepository.findByApartmentIdOrderByEventAtDesc(apartmentId);
        Set<Long> facilityIdSet = logs.stream()
                .map(SafetyEventLog::getFacilityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> dongIdSet = logs.stream()
                .map(SafetyEventLog::getDongId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> sensorIdSet = logs.stream()
                .map(SafetyEventLog::getSafetySensor)
                .filter(Objects::nonNull)
                .map(SafetySensor::getId)
                .collect(Collectors.toSet());

        Map<Long, String> facilityNameById = facilityRepository.findAllById(facilityIdSet).stream()
                .filter(facility -> facility.getApartment().getId().equals(apartmentId))
                .collect(Collectors.toMap(Facility::getId, Facility::getName));
        Map<Long, String> dongNoById = dongRepository.findAllById(dongIdSet).stream()
                .filter(dong -> dong.getApartment().getId().equals(apartmentId))
                .collect(Collectors.toMap(Dong::getId, Dong::getDongNo));
        Map<Long, String> sensorNameById = sensorRepository.findAllById(sensorIdSet).stream()
                .collect(Collectors.toMap(SafetySensor::getId, SafetySensor::getName));

        return logs.stream()
                .map(log -> {
                    boolean isManual = log.isManual();
                    String dongNo = log.getDongId() == null ? null : dongNoById.get(log.getDongId());
                    String facilityName = log.getFacilityId() == null ? null : facilityNameById.get(log.getFacilityId());
                    return new SafetyEventLogResponse(
                            log.getId(),
                            dongNo,
                            facilityName,
                            isManual,
                            log.getRequestFrom(),
                            log.getSafetySensor() == null ? null : sensorNameById.get(log.getSafetySensor().getId()),
                            isManual ? null : log.getSensorType(),
                            isManual ? null : log.getValue(),
                            isManual ? null : log.getUnit(),
                            log.getStatusTo(),
                            log.getEventAt()
                    );
                })
                .toList();
    }

    public List<SafetySensorLogResponse> listSafetySensorLogs(Long apartmentId) {
        if (apartmentId == null || apartmentId <= 0) {
            return List.of();
        }
        // 센서별 최신 로그 1건씩만 조회 (중복/과다 로그 방지)
        List<SafetySensorLog> logs = sensorLogRepository.findLatestPerSensorByApartmentId(apartmentId);
        Set<Long> sensorIdSet = logs.stream()
                .map(SafetySensorLog::getSafetySensor)
                .filter(Objects::nonNull)
                .map(SafetySensor::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, SafetySensor> sensorById = sensorRepository.findAllById(sensorIdSet).stream()
                .collect(Collectors.toMap(SafetySensor::getId, sensor -> sensor));

        Set<Long> facilityIdSet = sensorById.values().stream()
                .map(SafetySensor::getSpace)
                .filter(Objects::nonNull)
                .map(space -> space.getFacility())
                .filter(Objects::nonNull)
                .map(Facility::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> dongIdSet = sensorById.values().stream()
                .map(SafetySensor::getHo)
                .filter(Objects::nonNull)
                .map(ho -> ho.getDong())
                .filter(Objects::nonNull)
                .map(Dong::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> facilityNameById = facilityRepository.findAllById(facilityIdSet).stream()
                .filter(facility -> facility.getApartment().getId().equals(apartmentId))
                .collect(Collectors.toMap(Facility::getId, Facility::getName));
        Map<Long, String> dongNoById = dongRepository.findAllById(dongIdSet).stream()
                .filter(dong -> dong.getApartment().getId().equals(apartmentId))
                .collect(Collectors.toMap(Dong::getId, Dong::getDongNo));

        return logs.stream()
                .map(log -> {
                    Long sensorId = log.getSafetySensor().getId();
                    SafetySensor sensor = sensorById.get(sensorId);
                    String sensorName = sensor == null ? null : sensor.getName();
                    String facilityName = null;
                    String dongNo = null;
                    String hoNo = null;
                    String spaceName = null;

                    if (sensor != null && sensor.getSpace() != null && sensor.getSpace().getFacility() != null) {
                        facilityName = facilityNameById.get(sensor.getSpace().getFacility().getId());
                        spaceName = sensor.getSpace().getName();
                    }

                    if (sensor != null && sensor.getHo() != null && sensor.getHo().getDong() != null) {
                        dongNo = dongNoById.get(sensor.getHo().getDong().getId());
                        hoNo = sensor.getHo().getHoNo();
                    }

                    return new SafetySensorLogResponse(
                            sensorName,
                            dongNo,
                            hoNo,
                            facilityName,
                            spaceName,
                            log.getSafetySensor().getSensorType(),
                            log.getValue(),
                            log.getUnit(),
                            log.getRecordedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public SafetyLockResponse updateFacilityReservationLock(SafetyLockRequest request) {
        Facility facility = facilityRepository.findById(request.facilityId()).orElse(null);
        if (facility == null) {
            return null;
        }

        boolean reservationAvailable = Boolean.TRUE.equals(request.reservationAvailable());
        facility.changeReservationAvailability(reservationAvailable);

        Long apartmentId = facility.getApartment().getId();
        Long facilityId = facility.getId();

        SafetyStatus statusTo = reservationAvailable ? SafetyStatus.SAFE : SafetyStatus.DANGER;
        SafetyReason reason = reservationAvailable ? SafetyReason.MANUAL_UNLOCK : SafetyReason.MANUAL_LOCK;
        LocalDateTime now = LocalDateTime.now();

        SafetyStatusEntity statusEntity = safetyStatusRepository.findByApartmentIdAndFacilityId(apartmentId, facilityId)
                .orElseGet(() -> SafetyStatusEntity.builder()
                        .apartment(facility.getApartment())
                        .dongId(null)
                        .facilityId(facilityId)
                        .updatedAt(now)
                        .reason(reason)
                        .safetyStatus(statusTo)
                        .build());

        statusEntity.update(now, reason, statusTo);
        safetyStatusRepository.save(statusEntity);

        SafetyEventLog eventLog = SafetyEventLog.builder()
                .apartment(facility.getApartment())
                .dongId(null)
                .facilityId(facilityId)
                .manual(true)
                .requestFrom(currentAdminRequestFrom())
                .safetySensor(null)
                .sensorType(null)
                .value(null)
                .unit(null)
                .statusTo(statusTo)
                .eventAt(now)
                .build();
        safetyEventLogRepository.save(eventLog);

        return new SafetyLockResponse(facility.getName(), reservationAvailable, statusTo, reason);
    }

    @Transactional
    public void handleSafetySensor(String deviceId, SafetySensorInboundPayload payload) {
        if (payload.sensorType() != null && payload.sensorType().equalsIgnoreCase("GAS_DO")) {
            log.debug("MQTT safety ignored: GAS_DO payload deviceId={}, value={}", deviceId, payload.value());
            return;
        }
        SafetySensor safetySensor = resolveSafetySensor(deviceId);
        if (safetySensor == null) {
            log.warn("Unknown safety sensor deviceId={}", deviceId);
            return;
        }
        ScopeContext scopeContext = resolveScope(safetySensor);
        SensorType sensorType = parseSensorType(payload.sensorType());

        LocalDateTime eventAt = parseEventAt(payload.ts());

        // 중복 방지: 같은 센서의 마지막 로그와 값+타임스탬프가 동일하면 스킵
        Optional<SafetySensorLog> lastLog = sensorLogRepository.findTopBySafetySensorIdOrderByIdDesc(safetySensor.getId());
        if (lastLog.isPresent()) {
            SafetySensorLog prev = lastLog.get();
            boolean sameValue = prev.getValue() != null && prev.getValue().equals(payload.value());
            boolean sameTime = prev.getRecordedAt() != null && prev.getRecordedAt().equals(eventAt);
            if (sameValue && sameTime) {
                log.debug("Duplicate sensor log skipped: deviceId={}, value={}, time={}", deviceId, payload.value(), eventAt);
                return;
            }
        }

        SafetySensorLog sensorLog = SafetySensorLog.builder()
                .safetySensor(safetySensor)
                .value(payload.value())
                .unit(payload.unit())
                .recordedAt(eventAt)
                .build();
        sensorLogRepository.save(sensorLog);

        boolean isDanger = isDanger(sensorType, payload.value());
        SafetyStatus statusTo = isDanger ? SafetyStatus.DANGER : SafetyStatus.SAFE;
        SafetyReason reason = switch (sensorType) {
            case HEAT -> SafetyReason.HEAT;
            case GAS -> SafetyReason.GAS;
        };

        Optional<SafetyStatusEntity> existingStatus = scopeContext.facilityId() == null
                ? safetyStatusRepository.findByApartmentIdAndDongId(scopeContext.apartmentId(), scopeContext.dongId())
                : safetyStatusRepository.findByApartmentIdAndFacilityId(scopeContext.apartmentId(), scopeContext.facilityId());

        SafetyStatus previousStatus = existingStatus.map(SafetyStatusEntity::getSafetyStatus).orElse(null);

        SafetyStatusEntity statusEntity = existingStatus.orElseGet(() -> SafetyStatusEntity.builder()
                .apartment(scopeContext.apartment())
                .dongId(scopeContext.facilityId() == null ? scopeContext.dongId() : null)
                .facilityId(scopeContext.facilityId())
                .updatedAt(eventAt)
                .reason(reason)
                .safetyStatus(statusTo)
                .build());

        statusEntity.update(eventAt, reason, statusTo);
        safetyStatusRepository.save(statusEntity);

        boolean statusChanged = previousStatus == null || previousStatus != statusTo;
        if (statusChanged) {
            SafetyEventLog eventLog = SafetyEventLog.builder()
                    .apartment(scopeContext.apartment())
                    .dongId(scopeContext.dongId())
                    .facilityId(scopeContext.facilityId())
                    .manual(false)
                    .requestFrom(REQUEST_FROM_MQTT)
                    .safetySensor(safetySensor)
                    .sensorType(sensorType)
                    .value(payload.value())
                    .unit(payload.unit())
                    .statusTo(statusTo)
                    .eventAt(eventAt)
                    .build();
            safetyEventLogRepository.save(eventLog);

            if (isDanger && statusEntity.canSendAlert(eventAt)) {
                sendFireAlertToAllMembers(scopeContext, sensorType, safetySensor);
                statusEntity.recordAlert(eventAt);
                safetyStatusRepository.save(statusEntity);
            }
        }

        // 항상 프론트엔드에 업데이트 전송 (센서 값 변경 시마다)
        String hoNo = safetySensor.getHo() != null ? safetySensor.getHo().getHoNo() : null;
        String spaceName = safetySensor.getSpace() != null ? safetySensor.getSpace().getName() : null;

        publishSafetyUpdate(
            scopeContext,
            statusTo,
            reason,
            eventAt,
            safetySensor.getId(),
            hoNo,
            spaceName,
            safetySensor.getName(),
            sensorType,
            payload.value(),
            payload.unit(),
            eventAt
        );

        if (isDanger && scopeContext.facility() != null) {
            scopeContext.facility().changeReservationAvailability(false);
            facilityRepository.save(scopeContext.facility());
            log.info("Safety alert requested deviceId={}, scope={}", deviceId, scopeContext);
        }
    }

    private void sendFireAlertToAllMembers(ScopeContext scopeContext, SensorType sensorType, SafetySensor safetySensor) {
        Long apartmentId = scopeContext.apartmentId();
        if (apartmentId == null) return;

        String title;
        String body;
        if (sensorType == SensorType.HEAT) {
            title = "🔥 화재 경보";
            String location = resolveLocationLabel(scopeContext, safetySensor);
            body = location.isBlank()
                    ? "화재가 감지되었습니다! 즉시 대피하세요."
                    : location + "에서 화재가 감지되었습니다! 즉시 대피하세요.";
        } else {
            title = "⚠️ 가스 누출 경보";
            String location = resolveLocationLabel(scopeContext, safetySensor);
            body = location.isBlank()
                    ? "가스 누출이 감지되었습니다! 즉시 대피하세요."
                    : location + "에서 가스 누출이 감지되었습니다! 즉시 대피하세요.";
        }

        List<Member> members = memberRepository.findMembersWithPushTokenByApartmentId(apartmentId);
        if (members.isEmpty()) {
            log.info("화재 경보 FCM 전송 대상 없음: apartmentId={}", apartmentId);
            return;
        }

        List<PushMessageRequest> messages = members.stream()
                .map(member -> notificationService.sendNotification(
                        member.getPushToken(),
                        title,
                        body,
                        Map.of("type", "FIRE_ALERT", "apartmentId", String.valueOf(apartmentId))
                ))
                .filter(Objects::nonNull)
                .toList();

        notificationService.sendPushMessages(messages);
        log.info("화재 경보 FCM 전송: apartmentId={}, sensorType={}, recipients={}", apartmentId, sensorType, messages.size());
    }

    private String resolveLocationLabel(ScopeContext scopeContext, SafetySensor safetySensor) {
        if (scopeContext.facility() != null) {
            return scopeContext.facility().getName();
        }
        if (safetySensor.getHo() != null && safetySensor.getHo().getDong() != null) {
            return safetySensor.getHo().getDong().getDongNo() + "동 " + safetySensor.getHo().getHoNo() + "호";
        }
        return "";
    }

        private void publishSafetyUpdate(
            ScopeContext scopeContext,
            SafetyStatus statusTo,
            SafetyReason reason,
            LocalDateTime eventAt,
                Long sensorId,
                String hoNo,
                String spaceName,
                String sensorName,
                SensorType sensorType,
                Double value,
                String unit,
                LocalDateTime recordedAt
        ) {
        try {
            SafetyStatusResponse response = createSafetyStatusResponse(scopeContext, statusTo, reason, eventAt);
            SafetyMqttUpdatePayload mqttPayload = new SafetyMqttUpdatePayload(
                response.dongNo(),
                response.facilityName(),
                response.status(),
                response.reason(),
                response.updatedAt(),
                    sensorId,
                    hoNo,
                    spaceName,
                sensorName,
                sensorType,
                value,
                unit,
                recordedAt
            );
            String jsonPayload = objectMapper.writeValueAsString(mqttPayload);
            
            Message<String> message = MessageBuilder.withPayload(jsonPayload)
                    .setHeader("mqtt_topic", "hdc/frontend/safety/update")
                    .build();

            mqttOutboundChannel.send(message);
//            log.info("Published safety update to MQTT: apartmentId={}, status={}", scopeContext.apartmentId(), statusTo);
        } catch (Exception e) {
            log.error("Failed to publish safety update to MQTT", e);
        }
    }

    private SafetyStatusResponse createSafetyStatusResponse(ScopeContext scopeContext, SafetyStatus statusTo, SafetyReason reason, LocalDateTime eventAt) {
        String dongNo = null;
        String facilityName = null;

        if (scopeContext.dongId() != null) {
            dongNo = dongRepository.findById(scopeContext.dongId())
                    .map(Dong::getDongNo)
                    .orElse(null);
        }

        if (scopeContext.facilityId() != null) {
            facilityName = facilityRepository.findById(scopeContext.facilityId())
                    .map(Facility::getName)
                    .orElse(null);
        }

        return new SafetyStatusResponse(dongNo, facilityName, statusTo, reason, eventAt);
    }

    private static String currentAdminRequestFrom() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private SafetySensor resolveSafetySensor(String deviceId) {
        Long id = Long.parseLong(deviceId);
        return sensorRepository.findById(id).orElse(null);
    }

    private ScopeContext resolveScope(SafetySensor safetySensor) {
        Facility facility = safetySensor.getSpace() != null
                ? safetySensor.getSpace().getFacility()
                : null;

        if (facility != null) {
            return new ScopeContext(facility.getApartment(), null, facility.getId(), facility);
        }

        Dong dong = safetySensor.getHo().getDong();
        return new ScopeContext(dong.getApartment(), dong.getId(), null, null);
    }

    private SensorType parseSensorType(String sensorType) {
        return SensorType.valueOf(sensorType.trim().toUpperCase());
    }

    private LocalDateTime parseEventAt(String ts) {
        return OffsetDateTime.parse(ts).toLocalDateTime();
    }

    private boolean isDanger(SensorType sensorType, Double value) {
        if (value == null) {
            return false;
        }
        return switch (sensorType) {
            case HEAT -> value >= HEAT_DANGER_THRESHOLD;
            case GAS -> value >= GAS_DANGER_THRESHOLD;
        };
    }

    private record ScopeContext(com.backend.nova.apartment.entity.Apartment apartment,
                                Long dongId,
                                Long facilityId,
                                Facility facility) {
        Long apartmentId() {
            return apartment != null ? apartment.getId() : null;
        }
    }
}
