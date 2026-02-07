package com.backend.nova.member.service;

import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.jwt.JwtToken;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.member.dto.*;
import com.backend.nova.member.entity.LoginType;
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
import java.util.UUID;

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
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN); // 400 Bad Request
        }

        // 2. 토큰에서 사용자 ID 추출
        String loginId = jwtProvider.getSubject(refreshToken);

        // 3. 사용자 존재 여부 확인
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)); // 404 NOT_FOUND

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
    public TokenResponse registerMember(SignupRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            if (request.loginType() != LoginType.NORMAL) {
                Member existingMember = memberRepository.findByLoginId(request.loginId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

                existingMember.updateOAuthInfo(request.loginType().name(), request.loginId(), request.profileImg());

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        existingMember.getLoginId(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("MEMBER"))
                );

                JwtToken jwtToken = jwtProvider.generateToken(authentication);

                return TokenResponse.builder()
                        .accessToken(jwtToken.accessToken())
                        .refreshToken(jwtToken.refreshToken())
                        .memberId(existingMember.getId())
                        .name(existingMember.getName())
                        .build();
            }
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID); // 409 Conflict 발생
        }

        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESIDENT_NOT_FOUND)); // 404 Not Found 발생

        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = request.toEntity(resident, encodedPassword);

        Member savedMember = memberRepository.save(member);

        // 회원가입 후 자동 로그인을 위한 토큰 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedMember.getLoginId(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("MEMBER"))
        );

        JwtToken jwtToken = jwtProvider.generateToken(authentication);

        return TokenResponse.builder()
                .accessToken(jwtToken.accessToken())
                .refreshToken(jwtToken.refreshToken())
                .memberId(savedMember.getId())
                .name(savedMember.getName())
                .build();
    }

    public MemberInfoResponse getMemberInfo(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)); // 404 NOT_FOUND
        return MemberInfoResponse.from(member);
    }

    public MemberApartmentResponse getMemberApartmentInfo(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)); // 404 NOT_FOUND
        return MemberApartmentResponse.from(member);
    }

    public FindIdResponse findMemberId(FindIdRequest request) {
        Member member = memberRepository.findByNameAndPhoneNumber(request.name(), request.phoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)); // 404 NOT_FOUND

        String maskedId = maskLoginId(member.getLoginId());

        return FindIdResponse.builder()
                .loginType(member.getLoginType())
                .message(member.getLoginType() == LoginType.NORMAL ?
                        "회원님의 아이디는 다음과 같습니다: " + maskedId :
                        member.getLoginType() + " 계정으로 가입된 회원입니다.")
                .build();
    }

    @Transactional
    public ResetPWResponse resetPassword(ResetPWRequest request) {
        // 1. 사용자 조회
        Member member = memberRepository.findByLoginIdAndNameAndPhoneNumber(
                request.loginId(), request.name(), request.phoneNumber()
        ).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)); // 404 NOT_FOUND

        // 2. OAuth 회원은 비밀번호 변경 불가 처리
        if (member.getLoginType() != LoginType.NORMAL) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_RESTRICTED); // 409 CONFLICT
        }

        // 3. 비밀번호 재설정 로직 (예: 임시 비밀번호 생성 후 업데이트 & 이메일 전송)
        String tempPassword = UUID.randomUUID().toString().substring(0, 8);
        member.updatePassword(passwordEncoder.encode(tempPassword)); // 비밀번호 변경

        return ResetPWResponse.builder()
                .tempPassword(tempPassword)
                .message("임시 비밀번호가 발급되었습니다. 복사하여 로그인해주세요.")
                .build();
    }

    /**
     * 비밀번호 변경 (현재 비밀번호 확인 포함)
     */
    @Transactional
    public void changePassword(String loginId, ChangePWRequest request) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)); // 404 NOT_FOUND

        // 1. 소셜 로그인 회원은 비밀번호 변경 불가
        if (member.getLoginType() != LoginType.NORMAL) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_RESTRICTED);
        }

        // 2. 현재 비밀번호 검증 (DB의 암호화된 비번 vs 입력받은 비번)
        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD); // 400 Bad Request
        }

        // 3. 새 비밀번호 암호화 및 저장
        member.updatePassword(passwordEncoder.encode(request.newPassword()));
    }

    private String maskLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return "";
        }

        if (loginId.length() <= 3) {
            // 아이디가 짧은 경우 (예: "ab" -> "a*")
            return loginId.charAt(0) + "*".repeat(loginId.length() - 1);
        }

        // 앞 5글자만 보여주고 나머지는 * 처리 (예: "userid123" -> "useri****")
        // 여기서는 실제 길이만큼 가리는 방식을 사용했습니다.
        return loginId.substring(0, 5) + "*".repeat(loginId.length() - 5);
    }
}