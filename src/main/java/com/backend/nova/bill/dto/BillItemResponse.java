package com.backend.nova.bill.dto;

import com.backend.nova.bill.entity.BillItemType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItemResponse {
    private Long id;
    private String name;
    private Integer price;
    private BillItemType itemType;
}
