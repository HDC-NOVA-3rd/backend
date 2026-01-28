package com.backend.nova.safety.service;

import com.backend.nova.apartment.repository.FacilityRepository;
import com.backend.nova.safety.dto.SafetyEventLogResponse;
import com.backend.nova.safety.entity.SafetyEventLog;
import com.backend.nova.safety.enums.SafetyStatus;
import com.backend.nova.safety.enums.SensorType;
import com.backend.nova.safety.repository.SafetyEventLogRepository;
import com.backend.nova.safety.repository.SafetyStatusRepository;
import com.backend.nova.safety.repository.SensorLogRepository;
import com.backend.nova.safety.repository.SensorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SafetyServiceEventLogMappingTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private SafetyEventLogRepository safetyEventLogRepository;

    @Mock
    private SafetyStatusRepository safetyStatusRepository;

    @Mock
    private SensorLogRepository sensorLogRepository;

    @Mock
    private SensorRepository sensorRepository;

    @Mock
    private SafetyAutoLockPolicy autoLockPolicy;

    @InjectMocks
    private SafetyService safetyService;

    @Test
    void listSafetyEventLogs_usesManualColumnAndNullifiesSensorFieldsForManualEvents() {
        SafetyEventLog manualLog = SafetyEventLog.builder()
                .apartment(null)
                .dongId(101L)
                .facilityId(10L)
                .manual(true)
                .requestFrom("admin01")
                .sensor(null)
                .sensorType(SensorType.SMOKE)
                .value(12.34)
                .unit("ppm")
                .statusTo(SafetyStatus.DANGER)
                .eventAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        SafetyEventLog sensorLog = SafetyEventLog.builder()
                .apartment(null)
                .dongId(102L)
                .facilityId(11L)
                .manual(false)
                .requestFrom("55")
                .sensor(null)
                .sensorType(SensorType.SMOKE)
                .value(98.76)
                .unit("ppm")
                .statusTo(SafetyStatus.SAFE)
                .eventAt(LocalDateTime.of(2026, 1, 2, 12, 0))
                .build();

        given(safetyEventLogRepository.findByApartmentIdOrderByEventAtDesc(1L))
                .willReturn(List.of(manualLog, sensorLog));

        List<SafetyEventLogResponse> responses = safetyService.listSafetyEventLogs(1L);

        assertThat(responses).hasSize(2);

        SafetyEventLogResponse manualResponse = responses.get(0);
        assertThat(manualResponse.manual()).isTrue();
        assertThat(manualResponse.requestFrom()).isEqualTo("admin01");
        assertThat(manualResponse.sensorType()).isNull();
        assertThat(manualResponse.value()).isNull();
        assertThat(manualResponse.unit()).isNull();

        SafetyEventLogResponse sensorResponse = responses.get(1);
        assertThat(sensorResponse.manual()).isFalse();
        assertThat(sensorResponse.requestFrom()).isEqualTo("55");
        assertThat(sensorResponse.sensorType()).isEqualTo(SensorType.SMOKE);
        assertThat(sensorResponse.value()).isEqualTo(98.76);
        assertThat(sensorResponse.unit()).isEqualTo("ppm");

        then(safetyEventLogRepository).should().findByApartmentIdOrderByEventAtDesc(1L);
    }
}
