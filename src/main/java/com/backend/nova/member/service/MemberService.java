package com.backend.nova.member.service;

import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.auth.member.MemberAuthenticationProvider;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.auth.member.MemberDetailsService;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.CustomAuthenticationException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.member.dto.*;
import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.oauth2.repository.AuthCodeInMemoryRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ResidentRepository residentRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberAuthenticationProvider memberAuthenticationProvider;
    private final JwtProvider jwtProvider;
    private final AuthCodeInMemoryRepository authCodeRepository;
    private final MemberDetailsService memberDetailsService;
    private final RedisTokenService redisTokenService;

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        // 1. Refresh Token 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN); // 400
        }

        // 2. 토큰에서 LoginID 추출
        String loginId = jwtProvider.getSubject(refreshToken);

        // Redis에 저장된 RT와 일치하는지 검증
        String redisRt = redisTokenService.getRefreshToken(loginId);
        if (redisRt == null || !redisRt.equals(refreshToken)) {
            throw new CustomAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. LoginID 기반 memberDetails 생성
        MemberDetails memberDetails = (MemberDetails) memberDetailsService.loadUserByUsername(loginId);

        // 4. 새로운 인증 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                memberDetails,
                null,
                memberDetails.getAuthorities()
        );
        // 새로운 인증 객체 기반 Access, Refresh 재발급
        TokenResponse newTokenResponse = jwtProvider.createTokenDto(authentication, memberDetails.getMemberId(), memberDetails.getName());

        // [RTR 적용] 새 토큰으로 덮어쓰기
        saveTokensToRedis(newTokenResponse, memberDetails);
        return newTokenResponse;
    }

    @Transactional
    public TokenResponse login(LoginRequest loginRequest) {
        // 입력된 ID, PW 기반으로 검증되지 않은 토큰 생성
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.loginId(), loginRequest.password());

        // 커스텀 Provider를 통해 직접 인증 처리 (Manager를 거치지 않아 순환참조 방지)
        Authentication authentication = memberAuthenticationProvider.authenticate(authenticationToken);

        MemberDetails memberDetails = (MemberDetails) authentication.getPrincipal();

        TokenResponse tokenResponse = jwtProvider.createTokenDto(authentication,memberDetails.getMemberId(),memberDetails.getName());
        //Access, Refresh 토큰 정보들을 Redis 저장소에 저장하기
        saveTokensToRedis(tokenResponse, memberDetails);

        return tokenResponse;
    }

    @Transactional
    public Optional<MemberDetails> processOAuthMemberLogin(String email, String provider, String profileImg) {
        return memberRepository.findByEmailWithResidentInfo(email)
                .map(member -> {
                    // 1. 소셜 정보 업데이트
                    if(member.getLoginType() == LoginType.NORMAL)
                        member.updateOAuthInfo(provider, profileImg);

                    // 2. 이미 메모리에 로드된(Fetch Join) 연관관계에서 ID 추출 (추가 Select 쿼리 안 나감)
                    Long apartmentId = null;
                    Long hoId = null;

                    if (member.getResident() != null && member.getResident().getHo() != null) {
                        hoId = member.getResident().getHo().getId();
                        apartmentId = member.getResident().getHo().getDong().getApartment().getId();
                    }

                    // 3. MemberDetails 반환
                    return new MemberDetails(member, apartmentId, hoId);
                });
    }

    @Transactional
    public AuthExchangeResponse exchangeAuthCode(String code) {
        // 1. 코드 조회 및 삭제 (One-Time Use)
        Object data = authCodeRepository.getAndRemove(code);

        if (data == null) {
            throw new CustomAuthenticationException(ErrorCode.INVALID_AUTH_CODE); // 400
        }

        // 2. 데이터 타입에 따라 응답 DTO 생성
        if (data instanceof TokenResponse) {
            // 로그인 성공 케이스 -> access, refresh 반환
            return AuthExchangeResponse.login((TokenResponse) data);
        } else if (data instanceof String) {
            // 회원가입 필요 케이스 -> register Token 할당
            return AuthExchangeResponse.register((String) data);
        }

        // 예기치 않은 데이터가 들어있는 경우
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR); //RunTime Exception, 500
    }

    @Transactional
    public TokenResponse registerMember(SignupRequest request) {
        // 기존 회원이 존재하는 지 확인
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID); // 409 Conflict 발생
        }

        // 실 입주민이 존재하는 지 확인
        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESIDENT_NOT_FOUND)); // 404 Not Found 발생

        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = request.toEntity(resident, encodedPassword);

        MemberLocationResponse locationDto = memberRepository.findApartmentIdByMemberId(member.getId())
                .orElse(null);
        Long apartmentId = (locationDto != null) ? locationDto.apartmentId() : null;
        Long hoId = (locationDto != null) ? locationDto.hoId() : null;

        memberRepository.save(member);

        MemberDetails memberDetails = new MemberDetails(member,apartmentId,hoId);

        // 회원가입 후 자동 로그인을 위한 토큰 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                memberDetails,
                null,
                memberDetails.getAuthorities()
        );
        TokenResponse tokenResponse = jwtProvider.createTokenDto(authentication,memberDetails.getMemberId(),memberDetails.getName());
        //Access, Refresh 토큰 정보들을 Redis 저장소에 저장하기
        saveTokensToRedis(tokenResponse, memberDetails);
        return tokenResponse;
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

    /**
     * Expo 푸시 토큰 저장 (로그인 시)
     */
    @Transactional
    public void savePushToken(Long memberId, String pushToken) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.updatePushToken(pushToken);
        log.info("push token 등록 완료");
    }

    /**
     * 로그아웃 : redis access / refresh token 삭제 + Expo 푸시 토큰 삭제
     */
    @Transactional
    public void logout(Long memberId, String loginId, String accessToken) {
        // 1. DB에서 Expo Push Token 삭제
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        member.updatePushToken(null);
        log.info("push token 해제 완료");

        // 2. Redis에서 Access Token 및 Refresh Token 삭제
        redisTokenService.deleteTokens(accessToken, loginId);
        log.info("회원(ID: {}) Redis 인증 토큰 삭제 완료", loginId);
    }

    // 공통 Redis 저장 메서드
    private void saveTokensToRedis(TokenResponse tokenResponse, MemberDetails memberDetails) {
        RedisMember dto = new RedisMember(
                memberDetails.getMemberId(), memberDetails.getUsername(), memberDetails.getName(),
                memberDetails.getApartmentId(), memberDetails.getHoId(), "MEMBER"
        );
        redisTokenService.saveAccessToken(tokenResponse.accessToken(), dto, jwtProvider.getAccessTokenExpires());
        redisTokenService.saveRefreshToken(memberDetails.getUsername(), tokenResponse.refreshToken(), jwtProvider.getRefreshTokenExpires());
    }
}