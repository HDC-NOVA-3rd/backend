package com.backend.nova.oauth2.dto;

import java.util.Map;

public record GoogleResponse(
        String id,
        String email,
        String name,
        String profileImage
) implements OAuth2Response{


    public static GoogleResponse from(Map<String, Object> attribute) {
        return new GoogleResponse(
                attribute.get("sub").toString(),
                attribute.get("email").toString(),
                attribute.get("name").toString(),
                attribute.get("picture").toString()
        );
    }

    @Override
    public String getProvider() {
        return "GOOGLE";
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
}
