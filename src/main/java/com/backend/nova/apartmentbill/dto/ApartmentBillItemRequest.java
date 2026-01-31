package com.backend.nova.apartmentbill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApartmentBillItemRequest(

        @NotBlank(message = "관리비 항목명은 필수입니다.")
        @Size(max = 50, message = "관리비 항목명은 50자 이내여야 합니다.")
        String name,

        @Size(max = 255, message = "설명은 255자 이내여야 합니다.")
        String description
) {
}
