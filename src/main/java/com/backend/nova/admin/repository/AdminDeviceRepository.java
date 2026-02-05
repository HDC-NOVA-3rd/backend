package com.backend.nova.admin.repository;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminDeviceRepository extends JpaRepository<AdminDevice, Long> {

    long countByAdminAndRevokedAtIsNull(Admin admin);

    List<AdminDevice> findByAdminAndRevokedAtIsNullOrderByCreatedAtAsc(Admin admin);

    Optional<AdminDevice> findByAdminAndDeviceIdAndRevokedAtIsNull(
            Admin admin, String deviceId
    );
}

