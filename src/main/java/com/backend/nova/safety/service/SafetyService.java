package com.backend.nova.safety.service;

import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.apartment.entity.Facility;
import com.backend.nova.apartment.repository.FacilityRepository;
import com.backend.nova.apartment.repository.DongRepository;
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
    private static final double SMOKE_DANGER_THRESHOLD = 500.0;
    private static final double HEAT_DANGER_THRESHOLD = 70.0;

    private final FacilityRepository facilityRepository;
    private final DongRepository dongRepository;
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
        List<SafetySensorLog> logs = sensorLogRepository.findBySafetySensor_Apartment_IdOrderByIdDesc(apartmentId);
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
                    Long facilityId = null;
                    String facilityName = null;
                    Long dongId = null;
                    String dongNo = null;

                    if (sensor != null && sensor.getSpace() != null && sensor.getSpace().getFacility() != null) {
                        Facility facility = sensor.getSpace().getFacility();
                        facilityId = facility.getId();
                        facilityName = facilityNameById.get(facilityId);
                    }

                    if (sensor != null && sensor.getHo() != null && sensor.getHo().getDong() != null) {
                        Dong dong = sensor.getHo().getDong();
                        dongId = dong.getId();
                        dongNo = dongNoById.get(dongId);
                    }

                    return new SafetySensorLogResponse(
                            sensorName,
                            dongNo,
                            facilityName,
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
        SafetySensor safetySensor = resolveSafetySensor(deviceId);
        ScopeContext scopeContext = resolveScope(safetySensor);
        SensorType sensorType = parseSensorType(payload.sensorType());

        LocalDateTime eventAt = parseEventAt(payload.ts());
        SafetySensorLog sensorLog = SafetySensorLog.builder()
                .safetySensor(safetySensor)
                .value(payload.value())
                .unit(payload.unit())
                .recordedAt(eventAt)
                .build();
        sensorLogRepository.save(sensorLog);

        boolean isDanger = isDanger(sensorType, payload.value());
        SafetyStatus statusTo = isDanger ? SafetyStatus.DANGER : SafetyStatus.SAFE;
        SafetyReason reason = sensorType == SensorType.SMOKE ? SafetyReason.FIRE_SMOKE : SafetyReason.HEAT;

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
        }

        if (isDanger && scopeContext.facility() != null) {
            scopeContext.facility().changeReservationAvailability(false);
            facilityRepository.save(scopeContext.facility());
            log.info("Safety alert requested deviceId={}, scope={}", deviceId, scopeContext);
        }
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
