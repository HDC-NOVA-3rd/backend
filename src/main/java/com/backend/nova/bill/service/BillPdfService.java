package com.backend.nova.bill.service;

import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillItem;
import com.backend.nova.bill.repository.BillRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class BillPdfService {

    private final BillRepository billRepository;

    public byte[] generateBillPdf(Long billId, Long hoId) {
        Bill bill = billRepository.findByIdAndHo_Id(billId, hoId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            // =============================
            // 제목
            // =============================
            Paragraph title = new Paragraph("관리비 고지서", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(Chunk.NEWLINE);

            // =============================
            // 기본 정보
            // =============================
            document.add(new Paragraph(
                    "고지 월: " + bill.getMonth(),
                    normalFont
            ));
            document.add(new Paragraph(
                    "세대 번호: " + bill.getHo().getHoNo(),
                    normalFont
            ));
            document.add(new Paragraph(
                    "고지서 번호: " + bill.getBillUid(),
                    normalFont
            ));

            document.add(Chunk.NEWLINE);

            // =============================
            // 항목 테이블
            // =============================
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{4, 2, 2});

            addHeader(table, "항목");
            addHeader(table, "금액");
            addHeader(table, "비고");

            for (BillItem item : bill.getItems()) {
                // 항목 이름
                table.addCell(item.getName());

                // 금액
                table.addCell(item.getPrice().toString());

                // 항목 타입 → 한글로 표시
                table.addCell(switch (item.getItemType()) {
                    case METER -> "계량기 기반 요금";
                    case MANAGEMENT -> "관리비 기본 항목";
                    case COMMUNITY -> "커뮤니티 시설 이용료";
                });
            }


            document.add(table);

            document.add(Chunk.NEWLINE);

            // =============================
            // 총액
            // =============================
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
