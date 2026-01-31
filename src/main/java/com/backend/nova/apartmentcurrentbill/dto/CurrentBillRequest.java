package com.backend.nova.apartmentcurrentbill.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentBillRequest {
    private Long hoId;
    private String billingMonth;
    private Integer totalAmount;
    private List<CurrentBillItemRequest> items;
}

