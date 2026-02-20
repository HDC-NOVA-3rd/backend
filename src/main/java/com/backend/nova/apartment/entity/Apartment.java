package com.backend.nova.apartment.entity;

import jakarta.persistence.*;
import lombok.*;
/**
 * 아파트(단지) 엔티티
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

    /**
     * 1. 단지별 고지서 데이터 생성일 (상태: OPEN)
     * 전월 사용량 검침이 완료되고 기초 데이터를 모으는 날짜
     * 시스템이 OPEN 고지서를 만들어 관리비 항목(커뮤니티 이용료 등)을 집계 시작.
     */
    @Column(nullable = false)
    @Builder.Default
    private int billGenerationDay = 15; // 기본 설정값일 뿐, 수정 가능해야 함

    /**
     * 2. 단지별 고지서 실제 발행일 (상태: READY)
     * 입주민에게 고지서가 공개되고 납부가 시작되는 날짜
     * 시스템이 READY로 바꾸며 입주민에게 공개. 마감일(dueDate) 확정
     */
    @Column(nullable = false)
    @Builder.Default
    private int billPublishDay = 25;
}