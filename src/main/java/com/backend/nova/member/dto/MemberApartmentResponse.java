package com.backend.nova.member.dto;

import com.backend.nova.member.entity.Member;
import lombok.Builder;

@Builder
public record MemberApartmentResponse(
        String apartmentName,
        String dongNo,
        String hoNo
) {
    public static MemberApartmentResponse from(Member member) {
        return MemberApartmentResponse.builder()
                .apartmentName(member.getResident().getHo().getDong().getApartment().getName())
                .dongNo(member.getResident().getHo().getDong().getDongNo())
                .hoNo(member.getResident().getHo().getHoNo())
                .build();
    }
}