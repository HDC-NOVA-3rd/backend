package com.backend.nova.safety.service;

import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.apartment.entity.Facility;
import com.backend.nova.apartment.repository.FacilityRepository;
import com.backend.nova.safety.dto.SafetySensorInboundPayload;
import com.backend.nova.safety.dto.SafetyEventLogResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SafetyService {
    private static final String REQUEST_FROM_UNKNOWN = "unknown";
    private static final String REQUEST_FROM_MQTT = "mqtt";
    private static final double SMOKE_DANGER_THRESHOLD = 500.0;
    private static final double HEAT_DANGER_THRESHOLD = 70.0;

    private final FacilityRepository facilityRepository;
    private final SafetyEventLogRepository safetyEventLogRepository;
    private final SafetyStatusRepository safetyStatusRepository;
    private final SensorLogRepository sensorLogRepository;
    private final SensorRepository sensorRepository;

    public List<SafetyStatusResponse> listSafetyStatus(Long apartmentId) {
        if (apartmentId == null || apartmentId <= 0) {
            return List.of();
        }
        List<SafetyStatusEntity> statusEntityList = safetyStatusRepository.findByApartmentIdOrderByUpdatedAtDesc(apartmentId);
        Set<Long> facilityIdSet = statusEntityList.stream()
                .map(SafetyStatusEntity::getFacilityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> facilityNameById = facilityRepository.findAllById(facilityIdSet).stream()
                .filter(facility -> facility.getApartment().getId().equals(apartmentId))
                .collect(Collectors.toMap(Facility::getId, Facility::getName));

        return statusEntityList.stream()
                .map(entity -> {
                    String facilityName = entity.getFacilityId() == null ? null : facilityNameById.get(entity.getFacilityId());
                    return new SafetyStatusResponse(
                            entity.getDongId(),
                            entity.getFacilityId(),
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
        return logs.stream()
                .map(log -> {
                    boolean isManual = log.isManual();
                    return new SafetyEventLogResponse(
                            log.getId(),
                            log.getDongId(),
                            log.getFacilityId(),
                            isManual,
                            log.getRequestFrom(),
                            isManual ? null : log.getSensorType(),
                            isManual ? null : log.getValue(),
                            isManual ? null : log.getUnit(),
                            log.getStatusTo(),
                            log.getEventedAt()
                    );
                })
                .toList();
    }

    public List<SafetySensorLogResponse> listSafetySensorLogs(Long apartmentId) {
        if (apartmentId == null || apartmentId <= 0) {
            return List.of();
        }
        return sensorLogRepository.findBySafetySensor_Apartment_IdOrderByIdDesc(apartmentId).stream()
                .map(log -> new SafetySensorLogResponse(
                        log.getId(),
                        log.getSafetySensor().getId(),
                        log.getSafetySensor().getSensorType(),
                        log.getValue(),
                        log.getSafetySensor().getCreatedAt()
                ))
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
                .eventedAt(now)
                .build();
        safetyEventLogRepository.save(eventLog);

        return new SafetyLockResponse(facility.getId(), reservationAvailable, statusTo, reason);
    }

    @Transactional
    public void handleSafetySensor(String deviceId, SafetySensorInboundPayload payload) {
        SafetySensor safetySensor = resolveSafetySensor(deviceId);
        if (safetySensor == null) {
            log.warn("Safety sensor not found for deviceId={}", deviceId);
            return;
        }

        ScopeContext scopeContext = resolveScope(safetySensor);
        if (scopeContext == null) {
            log.warn("Safety scope not found for deviceId={}", deviceId);
            return;
        }

        SensorType sensorType = parseSensorType(payload.sensorType());
        if (sensorType == null) {
            log.warn("Unsupported sensorType={} deviceId={}", payload.sensorType(), deviceId);
            return;
        }

        LocalDateTime eventedAt = parseEventedAt(payload.ts());
        SafetySensorLog sensorLog = SafetySensorLog.builder()
                .safetySensor(safetySensor)
                .value(payload.value())
                .build();
        sensorLogRepository.save(sensorLog);

        boolean isDanger = isDanger(sensorType, payload.value());
        SafetyStatus statusTo = isDanger ? SafetyStatus.DANGER : SafetyStatus.SAFE;
        SafetyReason reason = sensorType == SensorType.SMOKE ? SafetyReason.FIRE_SMOKE : SafetyReason.HEAT;

        SafetyStatusEntity statusEntity = scopeContext.facilityId() == null
                ? safetyStatusRepository.findByApartmentIdAndDongId(scopeContext.apartmentId(), scopeContext.dongId())
                .orElseGet(() -> SafetyStatusEntity.builder()
                        .apartment(scopeContext.apartment())
                        .dongId(scopeContext.dongId())
                        .facilityId(null)
                        .updatedAt(eventedAt)
                        .reason(reason)
                        .safetyStatus(statusTo)
                        .build())
                : safetyStatusRepository.findByApartmentIdAndFacilityId(scopeContext.apartmentId(), scopeContext.facilityId())
                .orElseGet(() -> SafetyStatusEntity.builder()
                        .apartment(scopeContext.apartment())
                        .dongId(null)
                        .facilityId(scopeContext.facilityId())
                        .updatedAt(eventedAt)
                        .reason(reason)
                        .safetyStatus(statusTo)
                        .build());

        statusEntity.update(eventedAt, reason, statusTo);
        safetyStatusRepository.save(statusEntity);

        if (isDanger) {
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
                    .eventedAt(eventedAt)
                    .build();
            safetyEventLogRepository.save(eventLog);

            if (scopeContext.facility() != null) {
                scopeContext.facility().changeReservationAvailability(false);
                facilityRepository.save(scopeContext.facility());
            }

            log.info("Safety alert requested deviceId={}, scope={}", deviceId, scopeContext);
        }
    }

    private static String currentAdminRequestFrom() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private SafetySensor resolveSafetySensor(String deviceId) {
        try {
            Long id = Long.parseLong(deviceId);
            return sensorRepository.findById(id).orElseGet(() -> sensorRepository.findByName(deviceId).orElse(null));
        } catch (NumberFormatException e) {
            return sensorRepository.findByName(deviceId).orElse(null);
        }
    }

    private ScopeContext resolveScope(SafetySensor safetySensor) {
        Facility facility = null;
        Long facilityId = null;
        Long dongId = null;
        com.backend.nova.apartment.entity.Apartment apartment = safetySensor.getApartment();

        if (safetySensor.getSpace() != null) {
            facility = safetySensor.getSpace().getFacility();
            if (facility != null) {
                facilityId = facility.getId();
                apartment = facility.getApartment();
            }
        }

        if (facilityId == null && safetySensor.getHo() != null) {
            Dong dong = safetySensor.getHo().getDong();
            if (dong != null) {
                dongId = dong.getId();
                apartment = dong.getApartment();
            }
        }

        if (facilityId == null && dongId == null) {
            return null;
        }

        return new ScopeContext(apartment, dongId, facilityId, facility);
    }

    private SensorType parseSensorType(String sensorType) {
        if (sensorType == null || sensorType.isBlank()) {
            return null;
        }
        try {
            return SensorType.valueOf(sensorType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDateTime parseEventedAt(String ts) {
        if (ts == null || ts.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(ts).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }

    private boolean isDanger(SensorType sensorType, Double value) {
        if (value == null) {
            return false;
        }
        return switch (sensorType) {
            case SMOKE -> value >= SMOKE_DANGER_THRESHOLD;
            case HEAT -> value >= HEAT_DANGER_THRESHOLD;
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
