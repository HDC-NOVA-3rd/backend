package com.backend.nova.member.service;

import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.jwt.JwtToken;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.member.dto.*;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberAuthenticationProvider memberAuthenticationProvider;
    private final JwtProvider jwtProvider;

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        // 1. Refresh Token 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 Refresh Token입니다.");
        }

        // 2. 토큰에서 사용자 ID 추출
        String loginId = jwtProvider.getSubject(refreshToken);

        // 3. 사용자 존재 여부 확인
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 4. 새로운 인증 객체 생성 (권한은 MEMBER 부여)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                member.getLoginId(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("MEMBER"))
        );

        // 5. Access Token만 새로 생성!
        String newAccessToken = jwtProvider.createAccessToken(authentication);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .memberId(member.getId())
                .name(member.getName())
                .build();
    }

    @Transactional
    public TokenResponse login(LoginRequest loginRequest) {
        // 입력된 ID, PW 기반으로 검증되지 않은 토큰 생성
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.loginId(), loginRequest.password());
        
        // 커스텀 Provider를 통해 직접 인증 처리 (Manager를 거치지 않아 순환참조 방지)
        Authentication authentication = memberAuthenticationProvider.authenticate(authenticationToken);

        JwtToken jwtToken = jwtProvider.generateToken(authentication);

        MemberDetails userDetails = (MemberDetails) authentication.getPrincipal();

        return TokenResponse.builder()
                .accessToken(jwtToken.accessToken())
                .refreshToken(jwtToken.refreshToken())
                .memberId(userDetails.getMemberId())
                .name(userDetails.getName())
                .build();
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

    public MemberInfoResponse getMemberInfo(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        return MemberInfoResponse.from(member);
    }

    public MemberApartmentResponse getMemberApartmentInfo(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        return MemberApartmentResponse.from(member);
    }
}