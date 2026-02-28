package com.backend.nova.oauth2.service;

import com.backend.nova.oauth2.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

// [시점] 소셜 서버(Google/Naver)로부터 사용자 정보를 성공적으로 받아왔을 때 실행된다.
@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    // 1. 소셜 API를 호출하여 JSON 데이터(이메일, 이름 등)를 가져옴
    // [예외 발생 가능] 소셜 서버 통신 실패 시 RestClientException 발생
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User;
        // 시간 측정 시작
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 소셜 로그인 API에서 유저 정보 가져오기
        try{
            oAuth2User = super.loadUser(userRequest);
        }
        catch (Exception e){
            // 외부 서버 통신 실패 시
            throw new OAuth2AuthenticationException(new OAuth2Error("server_error"), "소셜 로그인 서버와 통신에 실패했습니다.");
        }
        finally {
            // 시간 측정 종료 및 로그 출력
            stopWatch.stop();
            log.info("[OAuth2 통신 시간] Provider: {}, 소요시간: {} ms",
                    userRequest.getClientRegistration().getRegistrationId(),
                    stopWatch.getTotalTimeMillis());
        }

        log.info(String.valueOf(oAuth2User));

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response = null;

        // 2. 플랫폼마다 다른 JSON 구조를 서버 공통 Type(OAuth2Response)으로 변환
        if (registrationId.equals("naver")) {
            oAuth2Response = NaverResponse.from(oAuth2User.getAttributes());
        }
        else if (registrationId.equals("google")) {
            oAuth2Response = GoogleResponse.from(oAuth2User.getAttributes());
        }
        else {
            // [예외 발생 가능] application.yaml에 설정되지 않은 이상한 소셜 로그인 요청이 들어온 경우
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"),"지원하지 않는 소셜 로그인입니다.");
        }

        // 필수 정보(이메일)가 없는 경우 방어
        if (oAuth2Response.getEmail() == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_scope"), "이메일 정보가 필수입니다.");
        }

        // 3. CustomOAuth2User 객체 생성 및 반환 (DB 저장 X)
        // role은 임시로 GUEST 설정
        return new CustomOAuth2User(oAuth2Response, "ROLE_GUEST");

    }
}
