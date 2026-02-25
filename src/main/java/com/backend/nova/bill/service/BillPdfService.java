package com.backend.nova.bill.service;

import com.backend.nova.bill.entity.Bill;
import com.backend.nova.bill.entity.BillItem;
import com.backend.nova.bill.repository.BillRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Locale;

@Slf4j
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

            // =============================
            // 숫자 포맷터 (천 단위 콤마)
            // =============================
            NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);
            numberFormat.setGroupingUsed(true);
            numberFormat.setMaximumFractionDigits(0); // 소수점 제거 (필요 시 조정)

            // =============================
            // 폰트 로드
            // =============================
            byte[] fontBytes;
            try (InputStream is = new ClassPathResource("fonts/NanumGothic.ttf").getInputStream()) {
                fontBytes = is.readAllBytes();
            } catch (Exception e) {
                log.error("폰트 파일을 찾을 수 없습니다: {}", e.getMessage());
                throw new RuntimeException("Font file not found in resources/fonts/");
            }

            BaseFont baseFont = BaseFont.createFont(
                    "nanum.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    fontBytes,
                    null
            );

            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font normalFont = new Font(baseFont, 10, Font.NORMAL);
            Font tableHeaderFont = new Font(baseFont, 10, Font.BOLD);

            // =============================
            // 제목
            // =============================
            Paragraph title = new Paragraph("관리비 고지서", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("고지 월: " + bill.getBillMonth(), normalFont));
            document.add(new Paragraph("세대 번호: " + bill.getHo().getHoNo(), normalFont));
            document.add(new Paragraph("고지서 번호: " + bill.getBillUid(), normalFont));
            document.add(Chunk.NEWLINE);

            // =============================
            // 테이블
            // =============================
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{4, 2, 3});

            addHeader(table, "항목", tableHeaderFont);
            addHeader(table, "금액", tableHeaderFont);
            addHeader(table, "비고", tableHeaderFont);

            for (BillItem item : bill.getItems()) {

                table.addCell(new Phrase(item.getName(), normalFont));

                // 🔥 BigDecimal 안전 포맷 적용
                String formattedPrice = numberFormat.format(item.getPrice());
                table.addCell(new Phrase(formattedPrice + " 원", normalFont));

                String typeName = switch (item.getItemType()) {
                    case METER -> "계량기 기반";
                    case MANAGEMENT -> "일반 관리비";
                    case COMMUNITY -> "커뮤니티 이용";
                };

                table.addCell(new Phrase(typeName, normalFont));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            // =============================
            // 총액
            // =============================
            String formattedTotal = numberFormat.format(bill.getTotalPrice());
            Paragraph total = new Paragraph(
                    "총 납부 금액: " + formattedTotal + " 원",
                    titleFont
            );
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("PDF 생성 에러: ", e);
            throw new RuntimeException("PDF 생성 중 오류 발생: " + e.getMessage());
        }
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setFixedHeight(25f);
        table.addCell(cell);
    }
}