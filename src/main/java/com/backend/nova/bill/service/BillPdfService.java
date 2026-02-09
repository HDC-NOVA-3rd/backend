package com.backend.nova.bill.service;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillItem;
import com.backend.nova.bill.entity.BillStatus;
import com.backend.nova.bill.repository.BillRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillPdfService {

    private final BillRepository billRepository;

    // =============================
    // PK 기반
    // =============================
    public byte[] generateBillPdf(Long billId, Long hoId) {
        Bill bill = billRepository.findByIdAndHo_Id(billId, hoId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        return generatePdf(bill);
    }

    // =============================
    // UUID 기반
    // =============================
    public byte[] generateBillPdfByUuid(UUID billUuid, Authentication auth) {

        Bill bill = billRepository.findByBillUuid(billUuid)
                .orElseThrow(() -> new IllegalArgumentException("고지서를 찾을 수 없습니다."));

        if (bill.getStatus() == BillStatus.READY) {
            throw new IllegalStateException("아직 발행되지 않은 고지서입니다.");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof MemberDetails member) {
            if (!bill.getHo().getId().equals(member.getHoId())) {
                throw new AccessDeniedException("본인 고지서만 조회 가능합니다.");
            }
        }

        if (principal instanceof AdminDetails admin) {
            if (!bill.getHo().getDong().getApartment().getId()
                    .equals(admin.getApartmentId())) {
                throw new AccessDeniedException("해당 단지의 고지서만 조회 가능합니다.");
            }
        }

        return generatePdf(bill);
    }

    // =============================
    // 실제 PDF 생성 로직 (공통)
    // =============================
    private byte[] generatePdf(Bill bill) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            Paragraph title = new Paragraph("관리비 고지서", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("고지 월: " + bill.getMonth(), normalFont));
            document.add(new Paragraph("세대 번호: " + bill.getHo().getHoNo(), normalFont));
            document.add(new Paragraph("고지서 번호: " + bill.getBillUid(), normalFont));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{4, 2, 2});

            addHeader(table, "항목");
            addHeader(table, "금액");
            addHeader(table, "구분");

            for (BillItem item : bill.getItems()) {
                table.addCell(item.getName());
                table.addCell(item.getPrice() + " 원");
                table.addCell(item.getItemType().name());
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            Paragraph total = new Paragraph(
                    "총 납부 금액: " + bill.getTotalPrice() + " 원",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13)
            );
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}

