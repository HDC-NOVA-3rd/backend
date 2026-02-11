package com.backend.nova.bill.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BillGenerateRequest {

    @Pattern(
            regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "month는 YYYY-MM 형식이어야 합니다."
    )
    private String month;
}
