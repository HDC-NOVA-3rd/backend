package com.backend.nova.apartmentcurrentbill.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentBillResponse {
    private Long id;
    private Long hoId;
    private String billingMonth;
    private Integer totalAmount;
    private List<CurrentBillItemResponse> items;
}

