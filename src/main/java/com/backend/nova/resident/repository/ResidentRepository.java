package com.backend.nova.resident.repository;

import com.backend.nova.resident.entity.Resident;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    List<Resident> findByHo_Dong_Apartment_Id(Long apartmentId);

    @Modifying(clearAutomatically = true) // 연산 수행 후 영속성 컨텍스트를 비워라!
    @Query("DELETE FROM Resident r WHERE r.ho.id = :hoId")
    void deleteByHoId(Long hoId);


    // ho까지 같이 당겨오면(지연로딩 문제 예방) 편함
    @EntityGraph(attributePaths = {"ho"})
    Optional<Resident> findWithHoById(Long id);

}
