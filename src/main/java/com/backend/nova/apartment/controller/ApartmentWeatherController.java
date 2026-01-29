package com.backend.nova.apartment.controller;
import com.backend.nova.apartment.service.ApartmentWeatherService;
import com.backend.nova.weather.dto.OpenWeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/**
 * 단지(Apartment) 기준 외부 날씨/대기 정보 조회 컨트롤러
 *
 * - 프론트 홈 상단 "외부 날씨 카드"에서 사용
 * - apartmentId → (Service에서 단지 조회 및 좌표 추출) → OpenWeather API 호출
 * - 실시간 데이터이며 DB에 저장하지 않음
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apartment")
public class ApartmentWeatherController {

    private final ApartmentWeatherService apartmentWeatherService;

    // 단지 ID 기준 외부 날씨 조회
    @GetMapping("/{apartmentId}/weather")
    public OpenWeatherResponse getApartment(@PathVariable Long apartmentId) {
        return apartmentWeatherService.getApartmentWeather(apartmentId);
    }

}







