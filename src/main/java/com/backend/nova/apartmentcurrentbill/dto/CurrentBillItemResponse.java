package com.backend.nova.apartmentcurrentbill.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentBillItemResponse {
    private Long id;
    private String name;
    private Integer price;
    private String category;
}