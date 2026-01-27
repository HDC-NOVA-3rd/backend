package com.backend.nova.safety.entity;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.entity.Space;
import com.backend.nova.safety.enums.SensorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "sensor")
@Getter
@NoArgsConstructor
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 40, unique = true)
    private String name;

    @Column(name = "type", length = 40, nullable = false)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 20)
    private SensorType sensorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ho_id")
    private Ho ho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sensor other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Sensor{id=" + id + "}";
    }
}
