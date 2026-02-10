package com.backend.nova.management.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ManagementFeeUpdateRequest(

        @Size(max = 50)
        String name,

        @PositiveOrZero
        BigDecimal price,

        @Size(max = 255)
        String description
) {}

