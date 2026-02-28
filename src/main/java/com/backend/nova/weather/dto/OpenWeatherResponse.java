package com.backend.nova.weather.dto;


import java.io.Serializable;

public record OpenWeatherResponse (
    Integer temperature,
    Integer humidity,
    String airQuality,
    String locationName,
    String condition
) implements Serializable {}

