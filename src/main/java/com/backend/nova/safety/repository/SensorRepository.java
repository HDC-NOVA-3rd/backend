package com.backend.nova.safety.repository;

import com.backend.nova.safety.entity.Sensor;
import com.backend.nova.safety.enums.SensorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {
}
