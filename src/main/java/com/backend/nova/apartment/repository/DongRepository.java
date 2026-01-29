package com.backend.nova.apartment.repository;

import com.backend.nova.apartment.entity.Dong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DongRepository extends JpaRepository<Dong, Long> {
    Optional<Dong> findFirstByDongNo(String dongNo);
}
