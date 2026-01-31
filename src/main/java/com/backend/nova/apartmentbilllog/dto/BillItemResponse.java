package com.backend.nova.apartmentbilllog.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItemResponse {
    private Long id;
    private String name;
    private Integer price;
    private String category;
}
