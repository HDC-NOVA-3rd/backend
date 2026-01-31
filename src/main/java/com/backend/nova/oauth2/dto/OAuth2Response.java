package com.backend.nova.oauth2.dto;

public interface OAuth2Response {
    //제공자 (Ex. naver, google, ...)
    String getProvider();
    //제공자에서 발급해주는 아이디(번호)
    String getProviderId();
    //이메일
    String getEmail();
    //사용자 실명 (설정한 이름)
    String getName();
    //프로필 이미지 URL
    String getProfileImage();
    // [추가] 핸드폰 번호 (하이픈 제거된 형태 권장)
    default String getPhoneNumber() {
        return null;
    }

    // [추가] 생년월일 (YYYY-MM-DD 형태 권장)
    default String getBirthDate() {
        return null;
    }
}
