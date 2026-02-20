package com.backend.nova.resident.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.dto.ResidentRequest;
import com.backend.nova.resident.dto.ResidentResponse;
import com.backend.nova.resident.dto.ResidentVerifyResponse;
import com.backend.nova.resident.dto.SignupStatus;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResidentService {
    private final ResidentRepository residentRepository;
    private final HoRepository hoRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long createResident(ResidentRequest request, Long apartmentId) {

        Ho ho = hoRepository.findById(request.hoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.HO_NOT_FOUND));

        if (!ho.getDong().getApartment().getId().equals(apartmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Resident resident = Resident.builder()
                .ho(ho)
                .name(request.name())
                .phone(request.phone())
                .build();

        try {
            return residentRepository.save(resident).getId();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.RESIDENT_DUPLICATED);
        }
    }

    public ResidentResponse getResident(Long residentId, Long apartmentId) {

        Resident resident = residentRepository
                .findByIdAndHo_Dong_Apartment_Id(residentId, apartmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESIDENT_NOT_FOUND));

        return ResidentResponse.fromEntity(resident);
    }

    public List<ResidentResponse> getAllResidents(Long apartmentId) {
        return residentRepository.findByHo_Dong_Apartment_Id(apartmentId).stream()
                .map(ResidentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateResident(Long residentId, ResidentRequest request, Long apartmentId) {

        Resident resident = residentRepository
                .findByIdAndHo_Dong_Apartment_Id(residentId, apartmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESIDENT_NOT_FOUND));

        Ho ho = hoRepository.findById(request.hoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.HO_NOT_FOUND));

        if (!ho.getDong().getApartment().getId().equals(apartmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        resident.update(ho, request.name(), request.phone());
    }

    @Transactional
    public void deleteResident(Long residentId, Long apartmentId) {

        Resident resident = residentRepository
                .findByIdAndHo_Dong_Apartment_Id(residentId, apartmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESIDENT_NOT_FOUND));

        residentRepository.delete(resident);
    }

    @Transactional
    public void deleteAllResidents(Long hoId, Long apartmentId) {

        Ho ho = hoRepository.findById(hoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HO_NOT_FOUND));

        if (!ho.getDong().getApartment().getId().equals(apartmentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        residentRepository.deleteByHoId(hoId);
    }
    // 입주민 검증을 먼저 하고, 기존 가입 이력을 확인하는 메서드
    public ResidentVerifyResponse verifyResident(ResidentRequest request) {
        return residentRepository.findByHo_IdAndNameAndPhone(request.hoId(), request.name(), request.phone())
                .map(resident -> {
                    Optional<Member> existingMember = memberRepository.findByResident_Id(resident.getId());
                    // 가입된 Member 정보가 있는 경우 -> 가입된 계정이 있다고 안내
                    if (existingMember.isPresent()) {
                        Member member = existingMember.get();
                        log.info("가입된 정보 있음: {}",member);
                        return ResidentVerifyResponse.builder()
                                .isVerified(true)
                                .residentId(resident.getId())
                                .name(resident.getName())
                                .status(SignupStatus.ALREADY_EXISTS)
                                .loginType(member.getLoginType()) // "GOOGLE", "NORMAL", "NAVER", "KAKAO"
                                .build();
                    }

                    // Member 정보가 없는 경우 -> 가입 가능
                    log.info("가입된 멤버 정보 없음 -> 가입 가능: {}",resident);
                    return ResidentVerifyResponse.builder()
                            .isVerified(true)
                            .residentId(resident.getId())
                            .name(resident.getName())
                            .status(SignupStatus.AVAILABLE)
                            .build();
                })
                // 입주민 정보가 없는 경우 -> 가입 불가능
                .orElseGet(() -> {
                    log.info("입주민 리스트에 없는 정보 {}",request);
                    return ResidentVerifyResponse.builder()
                        .isVerified(false)
                        .status(SignupStatus.NOT_RESIDENT)
                        .build();
                });
    }
}
