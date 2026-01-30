package com.backend.nova.apartment.service;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.weather.dto.OpenWeatherResponse;
import com.backend.nova.weather.service.OpenWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 단지(Apartment) 기준 외부 날씨 조회 서비스
 *
 * - 컨트롤러에서 apartmentId만 받는다.
 * - DB에서 단지 조회 → 위도/경도 추출 → OpenWeatherService 호출
 * - 컨트롤러는 요청/응답만 담당하고, DB조회/처리는 이 서비스에서 담당한다.
 */
@Service
@RequiredArgsConstructor
public class ApartmentWeatherService {
    private final ApartmentRepository apartmentRepository;
    private final OpenWeatherService openWeatherService;

    public OpenWeatherResponse getApartmentWeather(Long apartmentId) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 단지를 찾을 수 없습니다. id=" + apartmentId));

        double lat = apartment.getLatitude();
        double lon = apartment.getLongitude();
        return openWeatherService.getOpenWeather(lat, lon);
    }

}
