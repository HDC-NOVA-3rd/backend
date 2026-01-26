package com.backend.nova.apartment.repository;

import com.backend.nova.apartment.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
}
