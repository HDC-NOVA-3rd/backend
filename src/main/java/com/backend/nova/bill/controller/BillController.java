package com.backend.nova.bill.controller;

import com.backend.nova.bill.dto.*;
import com.backend.nova.bill.service.BillPdfService;
import com.backend.nova.bill.service.BillService;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;


@Tag(name = "Bill", description = "고지서 조회 API")
@RestController
@RequestMapping("api/bill")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;
    private final BillPdfService billPdfService;

    // 고지서 리스트 조회 (관리자/사용자 분리)
    @Operation(summary = "고지서 리스트 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<Page<BillSummaryResponse>> getBills(
            @ModelAttribute BillSearchCondition condition, // 검색 조건
            Pageable pageable, // 페이징 (?page=0&size=10&sort=billMonth,desc)
            Authentication authentication) {

        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            return ResponseEntity.ok(billService.getBillsByApartment(admin.getApartmentId(), condition, pageable));
        } else if (principal instanceof MemberDetails member) {
            return ResponseEntity.ok(billService.getBillsByHo(member.getHoId(), pageable));
        }

        return ResponseEntity.status(403).build();
    }

    // 개별 고지서 상세 조회
    @Operation(summary = "개별 고지서 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{billId}")
    public ResponseEntity<BillDetailResponse> getBill(@PathVariable Long billId,
                                                      Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            return ResponseEntity.ok(billService.getBillForAdmin(billId, admin.getApartmentId()));
        } else if (principal instanceof MemberDetails member) {
            return ResponseEntity.ok(billService.getBillForMember(billId, member.getHoId()));
        }

        return ResponseEntity.status(403).build();
    }

    @Operation(summary = "고지서 엑셀 다운로드 데이터 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/excel")
    public ResponseEntity<List<BillSummaryResponse>> getBillsForExcel(
            @ModelAttribute BillSearchCondition condition,
            Authentication authentication) {

        Object principal = authentication.getPrincipal();

        if (principal instanceof AdminDetails admin) {
            List<BillSummaryResponse> data = billService.getAllBillsForExcel(admin.getApartmentId(), condition);
            return ResponseEntity.ok(data);
        }

        return ResponseEntity.status(403).build();
    }

    @Operation(summary = "고지서 PDF", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{billId}/pdf")
    public ResponseEntity<byte[]> downloadBillPdf(
            @PathVariable Long billId,
            Authentication authentication
    ) {
        MemberDetails member = (MemberDetails) authentication.getPrincipal();

        byte[] pdf = billPdfService.generateBillPdf(
                billId,
                member.getHoId()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=bill-" + billId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

}