package com.backend.nova.facility.entity;

import com.backend.nova.apartment.entity.Apartment;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "facility")
@Getter
@NoArgsConstructor
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "start_hour", nullable = false)
    private LocalTime startHour;

    @Column(name = "end_hour", nullable = false)
    private LocalTime endHour;

    @Column(name = "reservation_available", nullable = false)
    private boolean reservationAvailable;

    // mappedBy = "facility"는 Space.java의 필드명
    @OneToMany(mappedBy = "facility", fetch = FetchType.LAZY)
    private Set<Space> spaces = new LinkedHashSet<>();
    // mappedBy = "facility"는 FacilityImage.java의 필드명
    @OneToMany(mappedBy = "facility", fetch = FetchType.LAZY)
    private Set<FacilityImage> images = new LinkedHashSet<>();

    public void changeReservationAvailability(boolean available) {
        this.reservationAvailable = available;
    }
}
