package com.backend.nova.apartment.service;

import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.apartment.repository.HoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.backend.nova.apartment.dto.ApartmentStructure.*;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AptStructureService {

    private final ApartmentRepository apartmentRepository;
    private final DongRepository dongRepository;
    private final HoRepository hoRepository;

    // 모든 아파트 목록 조회
    public List<ApartmentResponse> getApartmentList() {
        return apartmentRepository.findAll().stream()
                .map(ApartmentResponse::from)
                .collect(Collectors.toList());
    }

    // 특정 아파트의 동 목록 조회
    public List<DongResponse> getDongListByApartmentId(Long apartmentId) {
        return dongRepository.findAllByApartmentId(apartmentId).stream()
                .map(DongResponse::from)
                .collect(Collectors.toList());
    }

    // 특정 동의 호 목록 조회
    public List<HoResponse> getHoListByDongId(Long dongId) {
        return hoRepository.findAllByDongId(dongId).stream()
                .map(HoResponse::from)
                .collect(Collectors.toList());
    }
}
