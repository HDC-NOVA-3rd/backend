package com.backend.nova.apartmentbilllog.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillRequest {
    private Long hoId;
    private String billingMonth;
    private Integer totalAmount;
    private List<BillItemRequest> items;
}

