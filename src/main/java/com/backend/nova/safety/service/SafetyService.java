package com.backend.nova.safety.service;

import com.backend.nova.apartment.entity.Facility;
import com.backend.nova.apartment.repository.FacilityRepository;
import com.backend.nova.safety.dto.SafetyEventLogResponse;
import com.backend.nova.safety.dto.SafetyLockRequest;
import com.backend.nova.safety.dto.SafetyLockResponse;
import com.backend.nova.safety.dto.SafetySensorLogResponse;
import com.backend.nova.safety.dto.SafetyStatusResponse;
import com.backend.nova.safety.entity.SafetyEventLog;
import com.backend.nova.safety.entity.SafetyStatusEntity;
import com.backend.nova.safety.entity.Sensor;
import com.backend.nova.safety.entity.SensorLog;
import com.backend.nova.safety.enums.SafetyReason;
import com.backend.nova.safety.enums.SafetyStatus;
import com.backend.nova.safety.enums.SensorType;
import com.backend.nova.safety.repository.SafetyEventLogRepository;
import com.backend.nova.safety.repository.SafetyStatusRepository;
import com.backend.nova.safety.repository.SensorLogRepository;
import com.backend.nova.safety.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafetyService {
    private static final String REQUEST_FROM_UNKNOWN = "unknown";

    private final FacilityRepository facilityRepository;
    private final SafetyEventLogRepository safetyEventLogRepository;
    private final SafetyStatusRepository safetyStatusRepository;
    private final SensorLogRepository sensorLogRepository;
    private final SensorRepository sensorRepository;
    private final SafetyAutoLockPolicy autoLockPolicy;

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
                            log.getEventAt()
                    );
                })
                .toList();
    }

    public List<SafetySensorLogResponse> listSafetySensorLogs(Long apartmentId) {
        if (apartmentId == null || apartmentId <= 0) {
            return List.of();
        }
        return sensorLogRepository.findBySensor_Apartment_IdOrderByIdDesc(apartmentId).stream()
                .map(log -> new SafetySensorLogResponse(
                        log.getId(),
                        log.getSensor().getId(),
                        log.getSensor().getSensorType(),
                        log.getValue()
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
                .sensor(null)
                .sensorType(null)
                .value(null)
                .unit(null)
                .statusTo(statusTo)
                .eventAt(now)
                .build();
        safetyEventLogRepository.save(eventLog);

        return new SafetyLockResponse(facility.getId(), reservationAvailable, statusTo, reason);
    }

    /**
     * 센서 값을 저장하고, 설정된 임계치 초과 시 자동으로 시설 예약을 차단합니다.
     * UNLOCK(해제)는 관리자 수동 조치로만 수행합니다.
     *
     * @return 위험 판단(임계치 초과) 시 true, 그 외 false
     */
    @Transactional
    public boolean autoLockFromSensor(Long sensorId, Double value) {
        return autoLockFromSensor(sensorId, value, LocalDateTime.now());
    }

    @Transactional
    public boolean autoLockFromSensor(Long sensorId, Double value, LocalDateTime eventAt) {
        if (sensorId == null || sensorId <= 0 || value == null) {
            return false;
        }
        Sensor sensor = sensorRepository.findById(sensorId).orElse(null);
        if (sensor == null) {
            return false;
        }

        sensorLogRepository.save(SensorLog.builder()
                .sensor(sensor)
                .value(value)
                .build());

        SensorType sensorType = sensor.getSensorType();
        if (!autoLockPolicy.isDangerous(sensorType, value)) {
            return false;
        }

        LocalDateTime occurredAt = eventAt == null ? LocalDateTime.now() : eventAt;
        SafetyReason reason = (sensorType == SensorType.SMOKE) ? SafetyReason.FIRE_SMOKE : SafetyReason.HEAT;
        SafetyStatus statusTo = SafetyStatus.DANGER;

        Facility facilityToLock = (sensor.getSpace() == null) ? null : sensor.getSpace().getFacility();
        Long facilityId = facilityToLock == null ? null : facilityToLock.getId();
        Long dongId = (facilityId != null || sensor.getHo() == null) ? null : sensor.getHo().getDong().getId();

        if (facilityToLock != null && Boolean.TRUE.equals(facilityToLock.getReservationAvailable())) {
            facilityToLock.changeReservationAvailability(false);
        }

        SafetyStatusEntity statusEntity;
        boolean statusExisted;
        if (facilityId != null) {
            var existing = safetyStatusRepository.findByApartmentIdAndFacilityId(sensor.getApartment().getId(), facilityId);
            statusExisted = existing.isPresent();
            statusEntity = existing.orElseGet(() -> SafetyStatusEntity.builder()
                    .apartment(sensor.getApartment())
                    .dongId(null)
                    .facilityId(facilityId)
                    .updatedAt(occurredAt)
                    .reason(reason)
                    .safetyStatus(statusTo)
                    .build());
        } else if (dongId != null) {
            var existing = safetyStatusRepository.findByApartmentIdAndDongId(sensor.getApartment().getId(), dongId);
            statusExisted = existing.isPresent();
            statusEntity = existing.orElseGet(() -> SafetyStatusEntity.builder()
                    .apartment(sensor.getApartment())
                    .dongId(dongId)
                    .facilityId(null)
                    .updatedAt(occurredAt)
                    .reason(reason)
                    .safetyStatus(statusTo)
                    .build());
        } else {
            return true;
        }

        boolean wasSameDanger = statusExisted
                && statusEntity.getSafetyStatus() == SafetyStatus.DANGER
                && statusEntity.getReason() == reason;
        statusEntity.update(occurredAt, reason, statusTo);
        safetyStatusRepository.save(statusEntity);

        if (!wasSameDanger) {
            safetyEventLogRepository.save(SafetyEventLog.builder()
                    .apartment(sensor.getApartment())
                    .dongId(dongId)
                    .facilityId(facilityId)
                    .manual(false)
                    .requestFrom(String.valueOf(sensorId))
                    .sensor(sensor)
                    .sensorType(sensorType)
                    .value(value)
                    .unit(autoLockPolicy.unitFor(sensorType))
                    .statusTo(statusTo)
                    .eventAt(occurredAt)
                    .build());
        }
        return true;
    }

    private static String currentAdminRequestFrom() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return REQUEST_FROM_UNKNOWN;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return REQUEST_FROM_UNKNOWN;
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String role = authority.getAuthority();
                    return "ROLE_ADMIN".equals(role) || "ROLE_SUPER_ADMIN".equals(role);
                });
        return isAdmin ? name : REQUEST_FROM_UNKNOWN;
    }

}
