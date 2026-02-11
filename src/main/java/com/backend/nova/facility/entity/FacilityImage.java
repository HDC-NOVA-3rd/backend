package com.backend.nova.facility.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "facility_image")
@Getter
@NoArgsConstructor
public class FacilityImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "display_order")
    private Integer displayOrder; // 이미지 순서 (1번이 대표 이미지)

    public FacilityImage(Facility facility, String imageUrl, Integer displayOrder) {
        this.facility = facility;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}