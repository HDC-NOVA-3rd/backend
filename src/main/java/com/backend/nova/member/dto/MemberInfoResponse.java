package com.backend.nova.member.dto;

import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.entity.Member;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record MemberInfoResponse(
        String name,
        String email,
        String phoneNumber,
        LocalDate birthDate,
        LoginType loginType,
        String profileImg
) {
    public static MemberInfoResponse from(Member member) {
        return MemberInfoResponse.builder()
                .name(member.getName())
                .email(member.getEmail())
                .phoneNumber(member.getPhoneNumber())
                .birthDate(member.getBirthDate())
                .loginType(member.getLoginType())
                .profileImg(member.getProfileImg())
                .build();
    }
}