package com.backend.nova.member.service;

import com.backend.nova.member.dto.RedisMember;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisTokenService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Access Token 저장 (Value: 유저 정보 JSON)
    public void saveAccessToken(String accessToken, RedisMember dto, long durationMs) {
        try {
            String jsonValue = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set("AT:" + accessToken, jsonValue, Duration.ofMillis(durationMs));
        } catch (Exception e) {
            throw new RuntimeException("Redis Access Token 저장 실패", e);
        }
    }
    // Refresh Token 저장 (Value: Refresh Token 문자열)
    public void saveRefreshToken(String loginId, String refreshToken, long durationMs) {
        redisTemplate.opsForValue().set("RT:" + loginId, refreshToken, Duration.ofMillis(durationMs));
    }

    // Access Token으로 유저 정보 조회
    public RedisMember getRedisMemberByAccessToken(String accessToken) {
        String jsonValue = (String) redisTemplate.opsForValue().get("AT:" + accessToken);
        if (jsonValue == null) return null;
        try {
            return objectMapper.readValue(jsonValue, RedisMember.class);
        } catch (Exception e) {
            throw new RuntimeException("Redis 인증 정보 파싱 실패", e);
        }
    }
    // Refresh Token 조회
    public String getRefreshToken(String loginId) {
        return (String) redisTemplate.opsForValue().get("RT:" + loginId);
    }

    // 로그아웃 시 토큰 삭제
    public void deleteTokens(String accessToken, String loginId) {
        redisTemplate.delete("AT:" + accessToken);
        redisTemplate.delete("RT:" + loginId);
    }
}
