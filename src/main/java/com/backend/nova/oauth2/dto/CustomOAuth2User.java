package com.backend.nova.oauth2.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * DB에 저장된 회원이 아니라, OAuth 인증 서버에서 받은 원본 데이터를 담는 객체입니다.
 * record를 사용하여 불변성을 보장하고 코드를 간소화했습니다.
 */
public record CustomOAuth2User(OAuth2Response oAuth2Response, String role) implements OAuth2User {

    @Override
    public Map<String, Object> getAttributes() {
        // OAuth2Response 구현 방식에 따라 Map을 반환하거나 null을 반환합니다.
        // 현재 로직(Handler)에서는 이 메서드를 직접 호출하지 않고 oAuth2Response를 사용하므로 null이어도 무방합니다.
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();

        // Lambda 표현식으로 간결하게 처리
        collection.add(() -> role);

        return collection;
    }

    @Override
    public String getName() {
        return oAuth2Response.getName();
    }

    /**
     * OAuthSuccessHandler 등에서 getOAuth2Response() 로 호출하기 위한 편의 메서드입니다.
     * (Java record는 기본적으로 필드명과 같은 oAuth2Response() 메서드를 생성하지만,
     * Getter 네이밍 컨벤션을 맞추기 위해 추가했습니다.)
     */
    public OAuth2Response getOAuth2Response() {
        return oAuth2Response;
    }
}
