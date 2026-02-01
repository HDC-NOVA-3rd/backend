package com.backend.nova.bill.controller;

import com.backend.nova.bill.service.BillPdfService;
import com.backend.nova.auth.member.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bill")
@RequiredArgsConstructor
public class BillPdfController {

    private final BillPdfService billPdfService;

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
