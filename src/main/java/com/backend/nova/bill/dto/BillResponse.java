package com.backend.nova.bill.dto;

import com.backend.nova.bill.entity.BillStatus;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillResponse {
    private Long id;
    private Long hoId;
    private String month;
    private Integer totalPrice;
    private BillStatus status;
    private List<BillItemResponse> items;
}

