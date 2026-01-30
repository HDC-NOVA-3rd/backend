package com.backend.nova.resident.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.resident.dto.ResidentRequest;
import com.backend.nova.resident.dto.ResidentResponse;
import com.backend.nova.resident.dto.ResidentVerifyResponse;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResidentService {
    private final ResidentRepository residentRepository;
    private final HoRepository hoRepository;

    @Transactional
    public Long createResident(ResidentRequest request) {
        Ho ho = hoRepository.findById(request.hoId())
                .orElseThrow(() -> new IllegalArgumentException("해당 호가 없습니다. id=" + request.hoId()));
        
        Resident resident = Resident.builder()
                .ho(ho)
                .name(request.name())
                .phone(request.phone())
                .build();
                
        return residentRepository.save(resident).getId();
    }

    public ResidentResponse getResident(Long residentId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 입주민이 없습니다. id=" + residentId));
        
        return ResidentResponse.fromEntity(resident);
    }

    public List<ResidentResponse> getAllResidents(Long apartmentId) {
        return residentRepository.findByHo_Dong_Apartment_Id(apartmentId).stream()
                .map(ResidentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateResident(Long residentId, ResidentRequest request) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 입주민이 없습니다. id=" + residentId));
        Ho ho = hoRepository.findById(request.hoId())
                .orElseThrow(() -> new IllegalArgumentException("해당 호가 없습니다. id=" + request.hoId()));
        resident.update(ho, request.name(), request.phone());
    }

    @Transactional
    public void deleteResident(Long residentId) {
        residentRepository.deleteById(residentId);
    }
    @Transactional
    public void deleteAllResidents(Long hoId) {
        if (!hoRepository.existsById(hoId)) {
            throw new IllegalArgumentException("해당 호가 없습니다. id=" + hoId);
        }
        residentRepository.deleteByHoId(hoId);
    }

    public ResidentVerifyResponse verifyResident(ResidentRequest request) {
        return residentRepository.findByHo_IdAndNameAndPhone(request.hoId(), request.name(), request.phone())
                .map(resident -> new ResidentVerifyResponse(true, resident.getId(), "인증 성공"))
                .orElseGet(() -> new ResidentVerifyResponse(false, null, "인증 실패"));
    }
}
