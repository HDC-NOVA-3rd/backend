package com.backend.nova.weather.service;

import com.backend.nova.weather.dto.OpenWeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * OpenWeatherMap 외부 API 연동 서비스
 *
 * - 위도(lat), 경도(lon)를 받아 현재 날씨 + 공기질 정보 조회
 * - 외부 API 응답을 우리 서비스 DTO(OpenWeatherResponse)로 변환
 * - DB 저장 없이 실시간 조회 용도
 */

// 위도/경도를 기준으로 외부 날씨 및 공기질 정보 조회
@Service
public class OpenWeatherService {
    private final WebClient webClient = WebClient.create();

    @Value("${openweather.api-key}")
    private String apiKey;

    // lat: 위도(latitude), lon: 경도(longitude)
    public OpenWeatherResponse getOpenWeather(double lat, double lon){

        // OpenWeatherMap 현재 날씨 API 호출 (온도, 습도, 지역명)
        Map weather = webClient.get() //GET 요청
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.openweathermap.org")
                        .path("/data/2.5/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        Map main =  (Map) weather.get("main");
        int temperature = ((Number) main.get("temp")).intValue();
        int humidity = ((Number) main.get("humidity")).intValue();
        String locationName = ((String) weather.get("name"));

        // OpenWeatherMap 공기질(AQI) API 호출
        Map air = webClient.get() //GET 요청
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.openweathermap.org")
                        .path("/data/2.5/air_pollution")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("appid", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        //리스트로 형태 배열로 받기
        List list = (List) air.get("list");
        Map first = (Map) list.get(0);
        Map mainAir =  (Map) first.get("main");
        int aqi = ((Number) mainAir.get("aqi")).intValue(); //1~5

        String airQuality = switch (aqi) {
            case 1 -> "좋음";
            case 2 -> "보통";
            case 3 -> "나쁨";
            case 4 -> "매우 나쁨";
            default -> "정보 없음";
        };
        return new OpenWeatherResponse(temperature, humidity, airQuality, locationName);
    }
}
