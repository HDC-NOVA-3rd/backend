package com.backend.nova.apartment.controller;

import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.weather.dto.OpenWeatherResponse;
import com.backend.nova.weather.service.OpenWeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/apartment")
public class ApartmentWeatherController {

    private final ApartmentRepository apartmentRepository;
    private final OpenWeatherService openWeatherService;

    @GetMapping("/{apartmentId}/weather")
    public OpenWeatherResponse getApartment(@PathVariable Long apartmentId) {
        Apartment apartment = apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 단지를 찾을 수 없습니다. id=" + apartmentId));
        double lat = apartment.getLatitude();
        double lon = apartment.getLongitude();
        return openWeatherService.getOpenWeather(lat, lon);
    }

}







