package com.backend.nova.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ManagementFeeCreateRequest(

        @NotBlank
        @Size(max = 50)
        String name,

        @NotNull
        @PositiveOrZero
        BigDecimal price,

        @Size(max = 255)
        String description
) {}

