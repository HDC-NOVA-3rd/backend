package com.backend.nova.facility.service;

import com.backend.nova.facility.dto.FacilityResponse;
import com.backend.nova.facility.entity.Facility;
import com.backend.nova.facility.repository.FacilityRepository;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {
    private final FacilityRepository facilityRepository;

    public FacilityResponse getFacility(Long facilityId) {
        Facility facility = facilityRepository.findByIdWithImages(facilityId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FACILITY_NOT_FOUND)); // 404
        return FacilityResponse.from(facility);
    }
}
