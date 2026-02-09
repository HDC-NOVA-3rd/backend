package com.backend.nova.bill.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

//고지서 발행용 DTO (요청)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BillReadyRequest {
    private Long hoId;
    private String month; // YYYY-MM
}

