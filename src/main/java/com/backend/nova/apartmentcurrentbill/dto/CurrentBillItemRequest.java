package com.backend.nova.apartmentcurrentbill.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentBillItemRequest {
    private String name;
    private Integer price;
    private String category;
}
