package com.backend.nova.safety.repository;

import com.backend.nova.safety.entity.SensorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorLogRepository extends JpaRepository<SensorLog, Long> {

    /**
     * 센서별 로그 조회 (최신순 - ID 역순)
     */
    List<SensorLog> findBySensorIdOrderByIdDesc(Long sensorId);

    /**
     * 센서별 최근 N개 로그 조회
     */
    List<SensorLog> findTop10BySensorIdOrderByIdDesc(Long sensorId);

    /**
     * 아파트별 센서 로그 조회 (최신순 - ID 역순)
     */
    @Query("""
            select sl
            from SensorLog sl
            join fetch sl.sensor s
            left join s.ho h
            left join h.dong d
            left join d.apartment a1
            left join s.space sp
            left join sp.facility f
            left join f.apartment a2
            where a1.id = :apartmentId or a2.id = :apartmentId
            order by sl.id desc
            """)
    List<SensorLog> findByApartmentIdOrderByIdDesc(@Param("apartmentId") Long apartmentId);
}
