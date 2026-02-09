package com.backend.nova.bill.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillRequest {
    private Long hoId;
    private String month;
    private Integer totalPrice;
    private List<BillItemRequest> items;
}

