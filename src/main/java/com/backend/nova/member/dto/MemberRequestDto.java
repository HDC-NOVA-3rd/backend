package com.backend.nova.member.dto;

import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.entity.Member;
import com.backend.nova.resident.entity.Resident;

import java.time.LocalDate;

public record MemberRequestDto(
        Long residentId,
        String loginId,
        String password,
        String email,
        String name,
        String phoneNumber,
        LocalDate birthDate,
        LoginType loginType,
        String profileImg
) {
    public Member toEntity(Resident resident, String encodedPassword) {
        return Member.builder()
                .resident(resident)
                .loginId(loginId)
                .password(encodedPassword)
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
                .birthDate(birthDate)
                .loginType(loginType != null ? loginType : LoginType.NORMAL)
                .profileImg(profileImg)
                .build();
    }
}