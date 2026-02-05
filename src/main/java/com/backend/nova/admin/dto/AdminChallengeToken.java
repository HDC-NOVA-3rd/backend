package com.backend.nova.admin.dto;

import com.backend.nova.admin.entity.AdminChallengePurpose;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminChallengeToken {
    private String loginId;
    private AdminChallengePurpose purpose;
}

