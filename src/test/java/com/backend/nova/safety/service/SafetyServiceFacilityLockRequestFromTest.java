package com.backend.nova.safety.service;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.entity.Facility;
import com.backend.nova.apartment.repository.FacilityRepository;
import com.backend.nova.safety.dto.SafetyLockRequest;
import com.backend.nova.safety.entity.SafetyEventLog;
import com.backend.nova.safety.repository.SafetyEventLogRepository;
import com.backend.nova.safety.repository.SafetyStatusRepository;
import com.backend.nova.safety.repository.SensorLogRepository;
import com.backend.nova.safety.repository.SensorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SafetyServiceFacilityLockRequestFromTest {

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateFacilityReservationLock_setsRequestFromAdminNameWhenRoleAdmin() {
        Long facilityId = 10L;
        Apartment apartment = Apartment.builder()
                .id(1L)
                .name("apt")
                .address("addr")
                .zipcode(null)
                .build();

        Facility facility = org.mockito.Mockito.mock(Facility.class);
        given(facility.getId()).willReturn(facilityId);
        given(facility.getApartment()).willReturn(apartment);
        given(facilityRepository.findById(facilityId)).willReturn(Optional.of(facility));
        given(safetyStatusRepository.findByApartmentIdAndFacilityId(apartment.getId(), facilityId)).willReturn(Optional.empty());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin01",
                        "",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        safetyService.updateFacilityReservationLock(new SafetyLockRequest(facilityId, false));

        ArgumentCaptor<SafetyEventLog> eventLogCaptor = ArgumentCaptor.forClass(SafetyEventLog.class);
        then(safetyEventLogRepository).should().save(eventLogCaptor.capture());
        SafetyEventLog savedLog = eventLogCaptor.getValue();

        assertThat(savedLog.isManual()).isTrue();
        assertThat(savedLog.getRequestFrom()).isEqualTo("admin01");
    }
}
