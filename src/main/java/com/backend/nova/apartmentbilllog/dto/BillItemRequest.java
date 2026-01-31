package com.backend.nova.apartmentbilllog.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItemRequest {
    private String name;
    private Integer price;
    private String category;
}
