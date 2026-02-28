package com.backend.nova.apartment.service;

import com.backend.nova.facility.dto.FacilityResponse;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.facility.repository.FacilityRepository;
import com.backend.nova.apartment.repository.HoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.backend.nova.apartment.dto.ApartmentStructure.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AptStructureService {

    private final ApartmentRepository apartmentRepository;
    private final DongRepository dongRepository;
    private final HoRepository hoRepository;
    private final FacilityRepository facilityRepository;

    // 모든 아파트 목록 조회
    public List<ApartmentResponse> getApartmentList() {
        return apartmentRepository.findAll().stream()
                .map(ApartmentResponse::from)
                .toList();
    }

    // 특정 아파트의 동 목록 조회
    public List<DongResponse> getDongListByApartmentId(Long apartmentId) {
        return dongRepository.findAllByApartmentId(apartmentId).stream()
                .map(DongResponse::from)
                .toList();
    }

    // 특정 동의 호 목록 조회
    public List<HoResponse> getHoListByDongId(Long dongId) {
        return hoRepository.findAllByDongId(dongId).stream()
                .map(HoResponse::from)
                .toList();
    }

    // 특정 아파트의 시설 목록 조회
    public List<FacilityResponse> getFacilityListByApartmentId(Long apartmentId) {
        // 필요하다면 여기서 SecurityContextHolder를 통해 로그인한 유저의 아파트 ID와
        // 요청한 apartmentId가 같은지 검증하는 로직을 넣을 수 있습니다.

        return facilityRepository.findAllByApartmentId(apartmentId).stream()
                .map(FacilityResponse::from)
                .toList();
    }
}
