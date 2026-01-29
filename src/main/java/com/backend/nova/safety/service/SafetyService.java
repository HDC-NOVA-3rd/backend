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

    private static String currentAdminRequestFrom() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

}
