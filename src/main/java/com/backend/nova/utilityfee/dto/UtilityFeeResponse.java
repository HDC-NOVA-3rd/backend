package com.backend.nova.utilityfee.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityFeeResponse {
    private Long id;
    private String name;
    private Integer price;
    private String category;
}