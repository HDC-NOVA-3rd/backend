package com.backend.nova.resident.service;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.dto.*;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentQueryRepository;
import com.backend.nova.resident.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResidentService {

    private final ResidentRepository residentRepository; // 등록, 수정, 삭제용 (JPA)
    private final ResidentQueryRepository residentQueryRepository; // 복잡한 조회용 (QueryDSL)
    private final HoRepository hoRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long createResident(ResidentSaveRequest request, Long apartmentId) {
        // request.hoId() 대신 request.dongNo()와 request.hoNo()를 사용하도록 로직 변경

        Ho ho = hoRepository.findByDong_Apartment_IdAndDong_DongNoAndHoNo(
                        apartmentId, request.dong(), request.ho())
                .orElseThrow(() -> new BusinessException(ErrorCode.HO_NOT_FOUND));

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

        return ResidentResponse.from(resident);
    }

    @Transactional(readOnly = true)
    public Page<ResidentResponse> getAllResidents(Long apartmentId, Long dongId, String searchTerm, Pageable pageable) {
        // residentRepository가 아니라 residentQueryRepository를 사용!
        Page<Resident> residentPage = residentQueryRepository.findAllByFilters(apartmentId, dongId, searchTerm, pageable);

        return residentPage.map(ResidentResponse::from);
    }

    @Transactional
    public void updateResident(Long residentId, ResidentSaveRequest request, Long apartmentId) {
        Resident resident = residentRepository
                .findByIdAndHo_Dong_Apartment_Id(residentId, apartmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESIDENT_NOT_FOUND));

        // 수정 시에도 동/호수 텍스트로 Ho 엔티티 조회
        Ho ho = hoRepository.findByDong_Apartment_IdAndDong_DongNoAndHoNo(
                        apartmentId, request.dong(), request.ho())
                .orElseThrow(() -> new BusinessException(ErrorCode.HO_NOT_FOUND));

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
