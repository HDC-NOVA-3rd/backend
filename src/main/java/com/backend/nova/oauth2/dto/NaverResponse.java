package com.backend.nova.oauth2.dto;

import java.util.Map;

public record NaverResponse(
        String id,
        String email,
        String name,
        String profileImage,
        String phoneNumber,
        String birthDate
) implements OAuth2Response {

    // 정적 팩토리 메서드나 별도 생성자를 통해 Map 데이터를 필드로 변환
    public static NaverResponse from(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        // 1. 전화번호 파싱 (하이픈 제거)
        String rawMobile = (String) response.get("mobile");
        String cleanMobile = rawMobile != null ? rawMobile.replaceAll("-", "") : null;

        // 2. 생년월일 조합 (birthyear + birthday) -> YYYY-MM-DD
        String birthYear = (String) response.get("birthyear"); // "1999"
        String birthDay = (String) response.get("birthday");   // "07-21"
        String fullBirthDate = null;

        if (birthYear != null && birthDay != null) {
            fullBirthDate = birthYear + "-" + birthDay; // "1999-07-21"
        }

        return new NaverResponse(
                response.get("id").toString(),
                response.get("email").toString(),
                response.get("name").toString(),
                response.get("profile_image").toString(),
                cleanMobile,
                fullBirthDate
        );
    }
    @Override
    public String getProvider() {
        return "NAVER";
    }

    @Override
    public String getProviderId() {
        return id;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getProfileImage() {
        return profileImage;
    }
    @Override
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public String getBirthDate() {
        return birthDate;
    }
}
