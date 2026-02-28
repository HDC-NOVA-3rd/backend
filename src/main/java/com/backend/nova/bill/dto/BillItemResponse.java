package com.backend.nova.bill.dto;

import com.backend.nova.bill.entity.BillItemType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 접근 제한
@AllArgsConstructor
@Builder
public class BillItemResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private BillItemType itemType;
}