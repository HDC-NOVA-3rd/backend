package com.backend.nova.chat.repository;

import com.backend.nova.chat.entity.DeviceCommandLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceCommandLogRepository extends JpaRepository<DeviceCommandLog, String> {
}
