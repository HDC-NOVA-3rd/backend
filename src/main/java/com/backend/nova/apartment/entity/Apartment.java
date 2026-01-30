package com.backend.nova.apartment.entity;

import jakarta.persistence.*;
import lombok.*;
/**
 * 아파트(단지) 엔티티
 *
 * - 단지 기본 정보 + 외부 날씨 조회를 위한 위도/경도 포함
 * - latitude / longitude는 OpenWeather API 요청에 사용됨
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;
  
    // 위도
    @Column(nullable = false)
    private Double latitude;
  
    // 경도
    @Column(nullable = false)
    private Double longitude;
}