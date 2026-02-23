package com.backend.nova.resident.dto;

import com.backend.nova.resident.entity.Resident;

public record ResidentResponse(
        Long residentId,
        String apartmentName,
        String dongNo,
        String hoNo,
        String name,
        String phone
) {
    /**
     * Entity를 DTO로 변환
     * QueryDSL에서 Fetch Join으로 ho, dong, apartment를 한꺼번에 가져오므로
     * 이 메서드 호출 시 추가 쿼리가 발생하지 않습니다.
     */
    public static ResidentResponse from(Resident resident) {
        return new ResidentResponse(
                resident.getId(),
                resident.getHo().getDong().getApartment().getName(),
                resident.getHo().getDong().getDongNo(),
                resident.getHo().getHoNo(),
                resident.getName(),
                resident.getPhone()
        );
    }
}