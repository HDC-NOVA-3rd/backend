package com.backend.nova.resident.repository;

import com.backend.nova.resident.entity.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    List<Resident> findByHo_Dong_Apartment_Id(Long apartmentId);

    Optional<Resident> findByHo_IdAndNameAndPhone(Long hoId, String name, String phone);

    @Modifying(clearAutomatically = true) // 연산 수행 후 영속성 컨텍스트를 비워라!
    @Query("DELETE FROM Resident r WHERE r.ho.id = :hoId")
    void deleteByHoId(Long hoId);
}
