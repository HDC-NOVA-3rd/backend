package com.backend.nova.oauth2.service;

import com.backend.nova.oauth2.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 소셜 로그인 API에서 유저 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        log.info(String.valueOf(oAuth2User));

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response = null;

        // 플랫폼 별 분기 처리
        if (registrationId.equals("naver")) {
            oAuth2Response = NaverResponse.from(oAuth2User.getAttributes());
        }
        else if (registrationId.equals("google")) {
            oAuth2Response = GoogleResponse.from(oAuth2User.getAttributes());
        }
        else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        // CustomOAuth2User 객체 생성 및 반환 (DB 저장 X)
        // role은 임시로 GUEST 설정
        return new CustomOAuth2User(oAuth2Response, "ROLE_GUEST");

    }
}
