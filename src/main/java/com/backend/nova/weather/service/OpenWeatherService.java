    package com.backend.nova.weather.service;

    import com.backend.nova.weather.dto.OpenWeatherResponse;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;
    import org.springframework.web.reactive.function.client.WebClient;
    import reactor.core.publisher.Mono;

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
        public OpenWeatherResponse getOpenWeather(double lat, double lon) {

            // 1. 현재 날씨 요청 Mono (온도, 습도, 지역명)
            Mono<Map> weatherMono = webClient.get() //GET 요청
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("api.openweathermap.org").path("/data/2.5/weather")
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .queryParam("appid", apiKey)
                            .queryParam("units", "metric")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class);

            // 2. 공기질(AQI) 요청 Mono
            Mono<Map> airMono = webClient.get() //OpenWeather로 GET 요청
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("api.openweathermap.org").path("/data/2.5/air_pollution")
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .queryParam("appid", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class);

            // 3. 위치 geoList 요청 Mono
            Mono<List> geoMono = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("api.openweathermap.org").path("/geo/1.0/reverse")
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .queryParam("limit", 5)
                            .queryParam("appid", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(List.class);

            // 3가지 api 요청을 비동기 처리
            return Mono.zip(weatherMono, airMono, geoMono).map(tuple -> {
                Map weather = tuple.getT1();
                Map air = tuple.getT2();
                List geo = tuple.getT3();

                Map main = (Map) weather.get("main");

                int temperature = ((Number) main.get("temp")).intValue();
                int humidity = ((Number) main.get("humidity")).intValue();
                String condition = getCondition((List) weather.get("weather"));
                String airQuality = getAirQuality((List) air.get("list"));
                String locationName = getKoreanLocationName(geo, lat, lon);
                return new OpenWeatherResponse(temperature, humidity, airQuality, locationName, condition);
            }).block();
        }

        // Condition 데이터 파싱
        private String getCondition(List weatherList) {
            Map w0 = (Map) weatherList.get(0);
            String mainWeather = (String) w0.get("main");
            return switch (mainWeather) {
                case "Clear" -> "맑음";
                case "Clouds" -> "구름";
                case "Rain", "Drizzle" -> "비";
                case "Snow" -> "눈";
                case "Thunderstorm" -> "번개";
                case "Mist", "Fog", "Haze", "Smoke", "Dust", "Sand", "Ash", "Squall", "Tornado" -> "안개";
                default -> "정보 없음";
            };
        }

        // 공기질 데이터 파싱
        private String getAirQuality(List airList) {
            Map firstAir = (Map) airList.get(0);
            Map mainAir = (Map) firstAir.get("main");
            int aqi = ((Number) mainAir.get("aqi")).intValue();

            return switch (aqi) {
                case 1 -> "좋음";
                case 2 -> "보통";
                case 3 -> "나쁨";
                case 4, 5 -> "매우 나쁨";
                default -> "정보 없음";
            };
        }

        // Korea 위치 데이터 파싱
        private String getKoreanLocationName(List geo, double lat, double lon) {
            if (geo == null || geo.isEmpty()) return "위치 정보 없음";
            Map first = (Map) geo.get(0);

            // 1) state(시/도) + name(동/구/시) 조합 만들기
            String state = first.get("state") == null ? "" : first.get("state").toString();
            String name  = first.get("name")  == null ? "" : first.get("name").toString();

            // 2) 한국어 이름 있으면 우선 사용 (없으면 name 사용)
            String place = name;
            Map localNames = (Map) first.get("local_names");
            if (localNames != null && localNames.get("ko") != null) {
                String ko = localNames.get("ko").toString();
                if (!ko.isBlank()) place = ko;
            }
            if (place.isBlank()) return "위치 정보 없음";

            // 3) state가 있으면 "서울특별시 역삼동" 형태로
            return state.isBlank() ? place : (state + " " + place);
        }
    }
