package com.backend.nova.apartment.controller;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.weather.dto.OpenWeatherResponse;
import com.backend.nova.weather.service.OpenWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/**
 * 단지(Apartment) 기준 외부 날씨/대기 정보 조회 컨트롤러
 *
 * - 프론트 홈 상단 "외부 날씨 카드"에서 사용
 * - apartmentId → DB에서 위도/경도 조회 → OpenWeather API 호출
 * - 실시간 데이터이며 DB에 저장하지 않음
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apartment")
public class ApartmentWeatherController {

    private final ApartmentRepository apartmentRepository;
    private final OpenWeatherService openWeatherService;

    // 단지 ID 기준 외부 날씨 조회
    @GetMapping("/{apartmentId}/weather")
    public OpenWeatherResponse getApartment(@PathVariable Long apartmentId) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 단지를 찾을 수 없습니다. id=" + apartmentId));
        double lat = apartment.getLatitude();
        double lon = apartment.getLongitude();
        return openWeatherService.getOpenWeather(lat, lon);
    }

}







