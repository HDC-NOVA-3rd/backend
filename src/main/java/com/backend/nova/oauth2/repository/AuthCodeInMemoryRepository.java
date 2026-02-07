package com.backend.nova.oauth2.repository;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthCodeInMemoryRepository {

    // 동시성 문제를 방지하기 위해 ConcurrentHashMap 사용
    // Key: AuthCode (UUID), Value: TokenResponse (로그인) or RegisterToken (회원가입)
    private final Map<String, Object> storage = new ConcurrentHashMap<>();

    // 코드 저장
    public void save(String code, Object data) {
        storage.put(code, data);
    }

    // 코드 조회 및 즉시 삭제 (1회용)
    public Object getAndRemove(String code) {
        return storage.remove(code);
    }
}