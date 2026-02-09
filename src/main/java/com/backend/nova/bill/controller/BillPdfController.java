package com.backend.nova.bill.controller;

import com.backend.nova.bill.service.BillPdfService;
import com.backend.nova.auth.member.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bill")
@RequiredArgsConstructor
public class BillPdfController {

    private final BillPdfService billPdfService;

    // =============================
    // PK 기반 (내부용 / 테스트용)
    // =============================
    @GetMapping("/id/{billId}/pdf")
    public ResponseEntity<byte[]> downloadBillPdfById(
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

    // =============================
    // UUID 기반 (권장 / 운영용)
    // =============================
    @GetMapping("/{billUid}/pdf")
    public ResponseEntity<byte[]> downloadPdfByUuid(
            @PathVariable UUID billUuid,
            @RequestParam(defaultValue = "false") boolean preview,
            Authentication authentication
    ) {
        byte[] pdf = billPdfService.generateBillPdfByUuid(billUuid, authentication);

        String disposition = preview ? "inline" : "attachment";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=bill-" + billUuid + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
