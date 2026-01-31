package com.backend.nova.apartmentbill.controller;

import com.backend.nova.apartmentbill.dto.ApartmentBillItemRequest;
import com.backend.nova.apartmentbill.dto.ApartmentBillItemResponse;
import com.backend.nova.apartmentbill.service.ApartmentBillItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/billItem")
//@RequestMapping("/api/bill-item")
public class ApartmentBillItemController {

    //ApartmentBillItem vs BillItem 고민중
    private final ApartmentBillItemService apartmentBillItemService;

    /* ===== 단지별 관리비 항목 조회 ===== */
    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<ApartmentBillItemResponse>> findBillItemByApartment(
            @PathVariable Long apartmentId) {
        List<ApartmentBillItemResponse> items = apartmentBillItemService.getItemsByApartment(apartmentId);
        return ResponseEntity.ok(items);
    }

    /* ===== 관리비 항목 등록 ===== */
    @PostMapping("/apartment/{apartmentId}")
    public ResponseEntity<ApartmentBillItemResponse> createBillItem(
            @PathVariable Long apartmentId,
            @RequestBody ApartmentBillItemRequest request) {
        ApartmentBillItemResponse created = apartmentBillItemService.createItem(apartmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /* ===== 관리비 항목 수정 ===== */
    @PutMapping("/{billItemId}")
    public ResponseEntity<ApartmentBillItemResponse> updateBillItem(
            @PathVariable Long billItemId,
            @RequestBody ApartmentBillItemRequest request) {
        ApartmentBillItemResponse updated = apartmentBillItemService.updateItem(billItemId, request);
        return ResponseEntity.ok(updated);
    }

    /* ===== 관리비 항목 비활성화 ===== */
    @PatchMapping("/{billItemId}/deactivate")
    public ResponseEntity<Void> deactivateBillItem(@PathVariable Long billItemId) {
        apartmentBillItemService.deactivateItem(billItemId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
