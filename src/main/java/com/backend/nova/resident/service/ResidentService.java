package com.backend.nova.resident.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.resident.dto.ResidentRequestDto;
import com.backend.nova.resident.dto.ResidentResponseDto;
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
    public Long createResident(ResidentRequestDto requestDto) {
        Ho ho = hoRepository.findById(requestDto.hoId())
                .orElseThrow(() -> new IllegalArgumentException("해당 호가 없습니다. id=" + requestDto.hoId()));
        Resident resident = new Resident(ho, requestDto.name(), requestDto.phone());
        return residentRepository.save(resident).getId();
    }

    public ResidentResponseDto getResident(Long residentId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 입주민이 없습니다. id=" + residentId));
        return new ResidentResponseDto(resident);
    }

    public List<ResidentResponseDto> getAllResidents(Long apartmentId) {
        return residentRepository.findByHo_Dong_Apartment_Id(apartmentId).stream()
                .map(ResidentResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateResident(Long residentId, ResidentRequestDto requestDto) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 입주민이 없습니다. id=" + residentId));
        Ho ho = hoRepository.findById(requestDto.hoId())
                .orElseThrow(() -> new IllegalArgumentException("해당 호가 없습니다. id=" + requestDto.hoId()));
        resident.update(ho, requestDto.name(), requestDto.phone());
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
}
