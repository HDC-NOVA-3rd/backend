package com.backend.nova.safety.service;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.entity.Facility;
import com.backend.nova.apartment.entity.Space;
import com.backend.nova.apartment.repository.FacilityRepository;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SafetyServiceSensorAutoLockTest {

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
    void autoLockFromSensor_locksFacilityAndWritesStatusAndEventOnFirstDanger() {
        Long sensorId = 55L;
        Double value = 100.0;
        LocalDateTime eventAt = LocalDateTime.of(2026, 1, 28, 12, 0);

        Apartment apartment = Apartment.builder()
                .id(1L)
                .name("apt")
                .address("addr")
                .zipcode(null)
                .build();

        Facility facility = org.mockito.Mockito.mock(Facility.class);
        given(facility.getId()).willReturn(10L);
        given(facility.getReservationAvailable()).willReturn(true);

        Space space = org.mockito.Mockito.mock(Space.class);
        given(space.getFacility()).willReturn(facility);

        Sensor sensor = org.mockito.Mockito.mock(Sensor.class);
        given(sensor.getApartment()).willReturn(apartment);
        given(sensor.getSpace()).willReturn(space);
        given(sensor.getSensorType()).willReturn(SensorType.SMOKE);

        given(sensorRepository.findById(sensorId)).willReturn(Optional.of(sensor));
        given(autoLockPolicy.isDangerous(SensorType.SMOKE, value)).willReturn(true);
        given(autoLockPolicy.unitFor(SensorType.SMOKE)).willReturn("ppm");
        given(safetyStatusRepository.findByApartmentIdAndFacilityId(apartment.getId(), facility.getId())).willReturn(Optional.empty());

        boolean dangerous = safetyService.autoLockFromSensor(sensorId, value, eventAt);
        assertThat(dangerous).isTrue();

        then(facility).should().changeReservationAvailability(false);

        ArgumentCaptor<SensorLog> sensorLogCaptor = ArgumentCaptor.forClass(SensorLog.class);
        then(sensorLogRepository).should().save(sensorLogCaptor.capture());
        assertThat(sensorLogCaptor.getValue().getValue()).isEqualTo(value);

        ArgumentCaptor<SafetyStatusEntity> statusCaptor = ArgumentCaptor.forClass(SafetyStatusEntity.class);
        then(safetyStatusRepository).should().save(statusCaptor.capture());
        SafetyStatusEntity savedStatus = statusCaptor.getValue();
        assertThat(savedStatus.getSafetyStatus()).isEqualTo(SafetyStatus.DANGER);
        assertThat(savedStatus.getReason()).isEqualTo(SafetyReason.FIRE_SMOKE);
        assertThat(savedStatus.getFacilityId()).isEqualTo(10L);

        ArgumentCaptor<SafetyEventLog> eventCaptor = ArgumentCaptor.forClass(SafetyEventLog.class);
        then(safetyEventLogRepository).should().save(eventCaptor.capture());
        SafetyEventLog savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.isManual()).isFalse();
        assertThat(savedEvent.getRequestFrom()).isEqualTo(String.valueOf(sensorId));
        assertThat(savedEvent.getSensorType()).isEqualTo(SensorType.SMOKE);
        assertThat(savedEvent.getValue()).isEqualTo(value);
        assertThat(savedEvent.getUnit()).isEqualTo("ppm");
        assertThat(savedEvent.getStatusTo()).isEqualTo(SafetyStatus.DANGER);
        assertThat(savedEvent.getEventAt()).isEqualTo(eventAt);
    }
}
