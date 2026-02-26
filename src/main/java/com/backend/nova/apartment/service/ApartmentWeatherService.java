package com.backend.nova.apartment.service;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.weather.dto.OpenWeatherResponse;
import com.backend.nova.weather.service.OpenWeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 단지(Apartment) 기준 외부 날씨 조회 서비스
 *
 * - 컨트롤러에서 apartmentId만 받는다.
 * - DB에서 단지 조회 → 위도/경도 추출 → OpenWeatherService 호출
 * - 컨트롤러는 요청/응답만 담당하고, DB조회/처리는 이 서비스에서 담당한다.
 * - 호출한 OpenWeather는 Redis 에 30분간 저장되고, 최근 저장된 것이 있으면 호출한다.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class ApartmentWeatherService {
    private final ApartmentRepository apartmentRepository;
    private final OpenWeatherService openWeatherService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public OpenWeatherResponse getApartmentWeather(Long apartmentId) {
        String cacheKey = "weather:apartment:" + apartmentId;
        // Redis 캐시 조회
        try {
            String cachedJson = (String) redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                log.info("[Weather Cache Hit] apartmentId: {}", apartmentId);
                return objectMapper.readValue(cachedJson, OpenWeatherResponse.class);
            }
        } catch (Exception e) {
            log.error("Redis 캐시 조회 중 오류 발생: {}", e.getMessage());
            // 캐시 에러가 나더라도 서비스는 정상 동작해야 하므로 예외를 먹고 진행
        }

        log.info("[Weather Cache Miss] 외부 API 호출. apartmentId: {}", apartmentId);

        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 단지를 찾을 수 없습니다. id=" + apartmentId));

        double lat = apartment.getLatitude();
        double lon = apartment.getLongitude();
        OpenWeatherResponse response = openWeatherService.getOpenWeather(lat, lon);
        // Redis에 저장
        try {
            // Object -> String 직렬화
            String jsonValue = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, Duration.ofMinutes(30));
        } catch (Exception e) {
            log.error("Redis 캐시 저장 중 오류 발생: {}", e.getMessage());
        }

        return response;
    }

}
