package com.backend.nova.homeEnvironment.repository;

import com.backend.nova.homeEnvironment.entity.Device;
import com.backend.nova.homeEnvironment.entity.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    // roomId로 모든 디바이스를 찾기
    List<Device> findAllByRoom_Id(Long roomId);
    // deviceCode로 기기 하나 찾기
    Optional<Device> findByDeviceCode(String deviceCode);
    // "이 roomId 안에서" deviceCode로 찾기
    Optional<Device> findByRoom_IdAndDeviceCode(Long roomId, String deviceCode);

    // 특정 세대(hoId)에 속한 모든 디바이스 조회 (기본 모드 액션 생성용)
    List<Device> findAllByRoom_Ho_Id(Long hoId);

    // "이 roomId 안에서" deviceType로 찾기
    Optional<Device> findByRoom_IdAndType(Long roomId, DeviceType type);
}
