package com.backend.nova.safety.service;

import com.backend.nova.safety.repository.SafetyEventLogRepository;
import com.backend.nova.safety.repository.SafetyStatusRepository;
import com.backend.nova.safety.repository.SensorLogRepository;
import com.backend.nova.safety.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SafetyService {
    private final SafetyEventLogRepository safetyEventLogRepository;
    private final SafetyStatusRepository safetyStatusRepository;
    private final SensorLogRepository sensorLogRepository;
    private final SensorRepository sensorRepository;


}
