package com.backend.nova.bill.dto;

import com.backend.nova.bill.entity.BillItemType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItemRequest {
    private Long referenceId;
    private String name;
    private Integer price;
    private BillItemType itemType;
}
