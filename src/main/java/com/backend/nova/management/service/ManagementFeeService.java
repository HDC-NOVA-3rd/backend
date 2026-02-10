package com.backend.nova.management.service;


import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.management.dto.ManagementFeeCreateRequest;
import com.backend.nova.management.dto.ManagementFeeResponse;
import com.backend.nova.management.dto.ManagementFeeUpdateRequest;
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
    private final ManagementFeeRepository managementFeeRepository;

    /* ===== 조회 ===== */
    @Transactional(readOnly = true)
    public List<ManagementFeeResponse> getItems(
            Long apartmentId,
            Boolean active
    ) {
        List<ManagementFee> fees;

        if (active == null) {
            fees = managementFeeRepository.findByApartmentId(apartmentId); // 전체
        } else if (active) {
            fees = managementFeeRepository.findByApartmentIdAndActiveTrue(apartmentId);
        } else {
            fees = managementFeeRepository.findByApartmentIdAndActiveFalse(apartmentId);
        }

        return fees.stream()
                .map(ManagementFeeResponse::from)
                .toList();
    }


    /* ===== 등록 ===== */
    public ManagementFeeResponse createItem(Long apartmentId, ManagementFeeCreateRequest request) {
        if (managementFeeRepository.existsByApartmentIdAndNameAndActiveTrue(
                apartmentId, request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_MANAGEMENT_FEE_NAME);
        }

        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APARTMENT_NOT_FOUND));

        ManagementFee fee = ManagementFee.create(
                apartment,
                request.name(),
                request.price(),
                request.description()
        );

        return ManagementFeeResponse.from(
                managementFeeRepository.save(fee)
        );
    }

    /* ===== 수정 ===== */
    public ManagementFeeResponse updateItem(
            Long feeId,
            Long apartmentId,
            ManagementFeeUpdateRequest request
    ) {
        ManagementFee fee = findWithOwnership(feeId, apartmentId);

        fee.update(
                request.name(),
                request.price(),
                request.description()
        );

        return ManagementFeeResponse.from(fee);
    }


    /* ===== 삭제 ===== */
    public void deactivateItem(Long feeId, Long apartmentId) {
        ManagementFee fee = findWithOwnership(feeId, apartmentId);
        fee.deactivate();
    }

    /* ===== 복구 ===== */
    public void restoreItem(Long feeId, Long apartmentId) {
        ManagementFee fee = findWithOwnership(feeId, apartmentId);

        if (fee.isActive()) {
            throw new BusinessException(
                    ErrorCode.MANAGEMENT_FEE_ALREADY_ACTIVE
            );
        }


        boolean existsActiveSameName =
                managementFeeRepository.existsByApartmentIdAndNameAndActiveTrue(
                        apartmentId,
                        fee.getName()
                );

        if (existsActiveSameName) {
            throw new BusinessException(
                    ErrorCode.MANAGEMENT_FEE_RESTORE_CONFLICT
            );
        }

        fee.restore();
    }


    /* ===== 공통 ===== */
    private ManagementFee findWithOwnership(Long feeId, Long apartmentId) {
        ManagementFee fee = managementFeeRepository.findById(feeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MANAGEMENT_FEE_NOT_FOUND));

        if (!fee.getApartment().getId().equals(apartmentId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return fee;
    }
}


