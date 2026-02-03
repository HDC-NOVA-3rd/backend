package com.backend.nova.admin.dto;

import com.backend.nova.member.dto.MemberApartmentResponse;
import com.backend.nova.member.entity.Member;

public class AdminApartmentResponse {
    String apartmentName,
    String dongNo,
    String hoNo
) {
        public static AdminApartmentResponse from(Admin admin) {
            return AdminApartmentResponse.builder()
                    .apartmentName(admin.getResident().getHo().getDong().getApartment().getName())
                    .dongNo(admin.getResident().getHo().getDong().getDongNo())
                    .hoNo(admin.getResident().getHo().getHoNo())
                    .build();
        }
    }
