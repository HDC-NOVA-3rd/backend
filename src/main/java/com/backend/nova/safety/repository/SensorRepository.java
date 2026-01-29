package com.backend.nova.safety.repository;

import com.backend.nova.safety.entity.SafetySensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<SafetySensor, Long> {
    Optional<SafetySensor> findByName(String name);
}
