package com.backend.nova.facility.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "space_image")
@Getter
@NoArgsConstructor
public class SpaceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "display_order")
    private Integer displayOrder; // 이미지 순서 (1번이 대표 이미지)

    public SpaceImage(Space space, String imageUrl, Integer displayOrder) {
        this.space = space;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }
}