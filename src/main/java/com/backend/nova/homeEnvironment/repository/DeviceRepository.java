package com.backend.nova.homeEnvironment.repository;

import com.backend.nova.homeEnvironment.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    // roomId로 모든 디바이스를 찾기
    List<Device> findAllByRoom_Id(Long roomId);
    // deviceCode로 기기 하나 찾기
    Optional<Device> findByDeviceCode(String deviceCode);
    // "이 roomId 안에서" deviceCode로 찾기 (안전)
    Optional<Device> findByRoom_IdAndDeviceCode(Long roomId, String deviceCode);
}
