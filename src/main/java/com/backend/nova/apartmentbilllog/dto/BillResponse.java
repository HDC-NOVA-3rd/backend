package com.backend.nova.apartmentbilllog.dto;

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
    private String billingMonth;
    private Integer totalAmount;
    private boolean status;
    private List<BillItemResponse> items;
}

