package com.backend.nova.weather.service;

import com.backend.nova.weather.dto.OpenWeatherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
//OpenWeatherMap 서버에 HTTP 요청을 보내서 데이터를 가져오는 서비스
@Service
public class OpenWeatherService {
    private final WebClient webClient = WebClient.create();

    @Value("${openweather.api-key}")
    private String apiKey;

    // lat: 위도(latitude), lon: 경도(longitude)
    public OpenWeatherResponse getOpenWeather(double lat, double lon){

        // 현재 날씨 (온도/습도)
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

        // 2) 공기질 (AQI: 1~5)
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
