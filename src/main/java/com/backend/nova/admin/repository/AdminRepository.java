package com.backend.nova.admin.repository;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * 로그인 ID로 관리자 조회
     */
    Optional<Admin> findByLoginId(String loginId);

    /**
     * 로그인 ID + 이메일로 관리자 조회 (비밀번호 재설정용)
     */
    Optional<Admin> findByLoginIdAndEmail(String loginId, String email);

    /**
     * 이메일로 관리자 조회
     */
    Optional<Admin> findByEmail(String email);

    /**
     * 활성 상태 관리자만 조회
     */
    //Optional<Admin> findByLoginIdAndStatusActive(String loginId);
    // 메서드 이름에서 Active를 제거하고, 파라미터로 상태를 받습니다.
    Optional<Admin> findByLoginIdAndStatus(String loginId, AdminStatus status);

}
