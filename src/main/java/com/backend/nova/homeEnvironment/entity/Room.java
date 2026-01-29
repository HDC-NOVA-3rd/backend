package com.backend.nova.homeEnvironment.entity;

import com.backend.nova.apartment.entity.Ho;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "room", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ho_id", "name"})
})

public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ho_id", nullable = false)
    private Ho ho;

    @Column(nullable = false)
    private String name;

    @Builder
    public Room(Ho ho, String name){
        this.ho = ho;
        this.name = name;
    }
}