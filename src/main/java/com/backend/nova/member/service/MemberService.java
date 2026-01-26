package com.backend.nova.member.service;

import com.backend.nova.member.dto.MemberRequestDto;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long registerMember(MemberRequestDto requestDto) {
        if (memberRepository.existsByLoginId(requestDto.loginId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        Resident resident = residentRepository.findById(requestDto.residentId())
                .orElseThrow(() -> new IllegalArgumentException("해당 입주민 정보가 없습니다. id=" + requestDto.residentId()));

        String encodedPassword = passwordEncoder.encode(requestDto.password());
        Member member = requestDto.toEntity(resident, encodedPassword);

        return memberRepository.save(member).getId();
    }
}
