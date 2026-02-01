package com.backend.nova.utilityfee.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReadingRequest {
    private Long hoId;
    private String billingMonth;
    private Integer totalAmount;
    private List<UtilityFeeRequest> items;
}

