package com.backend.nova.weather.dto;


public record OpenWeatherResponse (
    Integer temperature,
    Integer humidity,
    String airQuality,
    String locationName
){}

