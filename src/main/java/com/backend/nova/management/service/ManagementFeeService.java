package com.backend.nova.management.service;


import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.management.dto.ManagementFeeRequest;
import com.backend.nova.management.dto.ManagementFeeResponse;
import com.backend.nova.management.entity.ManagementFee;
import com.backend.nova.management.repository.ManagementFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ManagementFeeService {

    private final ApartmentRepository apartmentRepository;
    private final ManagementFeeRepository billItemRepository;

    /* ===== 단지별 관리비 항목 조회 ===== */
    @Transactional(readOnly = true)
    public List<ManagementFeeResponse> getItemsByApartment(Long apartmentId) {
        return billItemRepository.findByApartmentIdAndActiveTrue(apartmentId)
                .stream()
                .map(ManagementFeeResponse::from)
                .toList();
    }

    /* ===== 관리비 항목 등록 ===== */
    public ManagementFeeResponse createItem(Long apartmentId, ManagementFeeRequest request) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 단지입니다."));

        ManagementFee billItem = ManagementFee.builder()
                .apartment(apartment)
                .name(request.name())
                .description(request.description())
                .build();


        return ManagementFeeResponse.from(billItemRepository.save(billItem));
    }

    /* ===== 관리비 항목 수정 ===== */
    public ManagementFeeResponse updateItem(Long billItemId, ManagementFeeRequest request) {
        ManagementFee billItem = billItemRepository.findById(billItemId)
                .orElseThrow(() -> new IllegalArgumentException("관리비 항목이 존재하지 않습니다."));

        // 엔티티 행위 메서드 호출
        billItem.update(request.name(), request.description());

        return ManagementFeeResponse.from(billItem);
    }

    /* ===== 관리비 항목 비활성화 ===== */
    public void deactivateItem(Long billItemId) {
        ManagementFee billItem = billItemRepository.findById(billItemId)
                .orElseThrow(() -> new IllegalArgumentException("관리비 항목이 존재하지 않습니다."));

        // 엔티티 행위 메서드 호출
        billItem.deactivate();
    }

    public ManagementFeeResponse updateItem(
            Long billItemId,
            Long apartmentId,
            ManagementFeeRequest request
    )


    private void validateApartmentOwnership(
            ManagementFee fee,
            Long apartmentId
    ) {
        if (!fee.getApartment().getId().equals(apartmentId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }


    public ManagementFeeResponse updateItem(
            Long billItemId,
            Long apartmentId,
            ManagementFeeRequest request
    ) {
        ManagementFee billItem = billItemRepository.findById(billItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MANAGEMENT_FEE_NOT_FOUND));

        validateApartmentOwnership(billItem, apartmentId);

        billItem.update(request.name(), request.description());
        return ManagementFeeResponse.from(billItem);
    }


    public void deactivateItem(Long billItemId, Long apartmentId) {
        ManagementFee billItem = billItemRepository.findById(billItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MANAGEMENT_FEE_NOT_FOUND));

        validateApartmentOwnership(billItem, apartmentId);
        billItem.deactivate();
    }


    //SUPER_ADMIN 예외 처리
    private void validateApartmentOwnership(
            ManagementFee fee,
            Long apartmentId,
            AdminDetails adminDetails
    ) {
        if (adminDetails.isSuperAdmin()) {
            return;
        }
        if (!fee.getApartment().getId().equals(apartmentId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }


}

