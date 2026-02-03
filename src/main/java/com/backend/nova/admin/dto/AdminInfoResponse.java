package com.backend.nova.admin.dto;

import com.backend.nova.member.dto.MemberInfoResponse;
import com.backend.nova.admin.entity.Admin;

import java.time.LocalDate;

public class AdminInfoResponse {
    String name,
    String email,
    String phoneNumber,
    LocalDate birthDate,
    String profileImg
) {
        public static MemberInfoResponse from(Admin admin) {
            return MemberInfoResponse.builder()
                    .name(admin.getName())
                    .email(admin.getEmail())
                    .phoneNumber(admin.getPhoneNumber())
                    .birthDate(admin.getBirthDate())
                    .profileImg(admin.getProfileImg())
                    .build();
        }
    }
