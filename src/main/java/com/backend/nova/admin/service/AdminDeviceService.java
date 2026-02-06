package com.backend.nova.admin.service;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminDevice;
import com.backend.nova.admin.repository.AdminDeviceRepository;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminDeviceService {

    private static final int MAX_DEVICE_COUNT = 5;

    private final AdminDeviceRepository adminDeviceRepository;

    public void registerDevice(Admin admin, String deviceId, String ip) {

        // 기존 기기 존재 여부 확인
        AdminDevice device = adminDeviceRepository
                .findByAdminAndDeviceId(admin, deviceId)
                .orElse(null);

        if (device != null) {
            // 폐기된 기기면 복구
            device.setRevokedAt(null);
            device.setLastIp(ip);
            device.setLastUsedAt(LocalDateTime.now());
            device.setLastVerifiedAt(LocalDateTime.now());
            return;
        }

        // 기기 수 제한
        long activeDeviceCount =
                adminDeviceRepository.countByAdminAndRevokedAtIsNull(admin);

        if (activeDeviceCount >= MAX_DEVICE_COUNT) {
            throw new BusinessException(ErrorCode.DEVICE_LIMIT_EXCEEDED);
        }

        // 신규 등록
        AdminDevice newDevice = AdminDevice.builder()
                .admin(admin)
                .deviceId(deviceId)
                .lastIp(ip)
                .trusted(true)
                .lastVerifiedAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        adminDeviceRepository.save(newDevice);
    }
}
