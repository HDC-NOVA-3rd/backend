package com.backend.nova.resident.repository;

import com.backend.nova.resident.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    List<Resident> findByHo_Dong_Apartment_Id(Long apartmentId);
}
