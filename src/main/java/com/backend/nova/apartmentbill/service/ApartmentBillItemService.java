package com.backend.nova.apartmentbill.service;


import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.apartmentbill.dto.ApartmentBillItemRequest;
import com.backend.nova.apartmentbill.dto.ApartmentBillItemResponse;
import com.backend.nova.apartmentbill.entity.ApartmentBillItem;
import com.backend.nova.apartmentbill.repository.ApartmentBillItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ApartmentBillItemService {

    private final ApartmentRepository apartmentRepository;
    private final ApartmentBillItemRepository billItemRepository;

    /* ===== 단지별 관리비 항목 조회 ===== */
    @Transactional(readOnly = true)
    public List<ApartmentBillItemResponse> getItemsByApartment(Long apartmentId) {
        return billItemRepository.findByApartmentIdAndActiveTrue(apartmentId)
                .stream()
                .map(ApartmentBillItemResponse::from)
                .toList();
    }

    /* ===== 관리비 항목 등록 ===== */
    public ApartmentBillItemResponse createItem(Long apartmentId, ApartmentBillItemRequest request) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 단지입니다."));

        ApartmentBillItem billItem = ApartmentBillItem.builder()
                .apartment(apartment)
                .name(request.name())
                .description(request.description())
                .build();


        return ApartmentBillItemResponse.from(billItemRepository.save(billItem));
    }

    /* ===== 관리비 항목 수정 ===== */
    public ApartmentBillItemResponse updateItem(Long billItemId, ApartmentBillItemRequest request) {
        ApartmentBillItem billItem = billItemRepository.findById(billItemId)
                .orElseThrow(() -> new IllegalArgumentException("관리비 항목이 존재하지 않습니다."));

        // 엔티티 행위 메서드 호출
        billItem.update(request.name(), request.description());

        return ApartmentBillItemResponse.from(billItem);
    }

    /* ===== 관리비 항목 비활성화 ===== */
    public void deactivateItem(Long billItemId) {
        ApartmentBillItem billItem = billItemRepository.findById(billItemId)
                .orElseThrow(() -> new IllegalArgumentException("관리비 항목이 존재하지 않습니다."));

        // 엔티티 행위 메서드 호출
        billItem.deactivate();
    }
}

