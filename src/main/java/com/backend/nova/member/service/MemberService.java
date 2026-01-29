package com.backend.nova.member.service;

import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.member.dto.LoginRequest;
import com.backend.nova.member.dto.SignupRequest;
import com.backend.nova.member.dto.TokenResponse;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public TokenResponse login(LoginRequest loginRequest) {
        Member member = memberRepository.findByLoginId(loginRequest.loginId())
                .orElseThrow(() -> new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(loginRequest.password(), member.getPassword())) {
            throw new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                member.getLoginId(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        return jwtProvider.generateToken(authentication);
    }

    @Transactional
    public Long registerMember(SignupRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new IllegalArgumentException("해당 입주민 정보가 없습니다. id=" + request.residentId()));

        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = request.toEntity(resident, encodedPassword);

        return memberRepository.save(member).getId();
    }
}
