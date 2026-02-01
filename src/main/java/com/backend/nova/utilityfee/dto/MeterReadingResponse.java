package com.backend.nova.utilityfee.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReadingResponse {
    private Long id;
    private Long hoId;
    private String billingMonth;
    private Integer totalAmount;
    private List<UtilityFeeResponse> items;
}

