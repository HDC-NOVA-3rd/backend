package com.backend.nova.voice.service;

import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoiceDeviceMemberResolver {

    private final MemberRepository memberRepository;

    public Long resolveMemberId(Long hoId) {
        if (hoId == null || hoId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Member member = memberRepository.findFirstByResident_Ho_IdOrderByIdAsc(hoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return member.getId();
    }
}
