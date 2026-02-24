package com.backend.nova.homeEnvironment.mode.repository;

import com.backend.nova.homeEnvironment.mode.entity.Mode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ModeRepository extends JpaRepository<Mode, Long> {

    // 특정 세대(hoId)의 모드 목록을 이름순으로 조회 (숨김된 모드는 제외)
    List<Mode> findAllByHo_IdAndIsVisibleTrueOrderByModeNameAsc(Long hoId);
    // 설정 화면에서 전체 모드 목록 조회
    List<Mode> findAllByHo_IdOrderByModeNameAsc(Long hoId);

    // 특정 세대(hoId)에 같은 이름(modeName)의 모드가 이미 존재하는지 체크 (중복 생성 방지)
    boolean existsByHo_IdAndModeName(Long hoId, String modeName);

    // 특정 세대(hoId) 안에서 특정 modeId 모드를 찾기 (다른 세대 모드 접근 차단용)
    Optional<Mode> findByIdAndHo_Id(Long modeId, Long hoId);

    // 특정 세대(hoId)의 기본 모드만 조회 (외출/취침/귀가 같은 기본 모드 리스트)
    List<Mode> findAllByHo_IdAndIsDefaultTrue(Long hoId);

    // 특정 세대(hoId)에서 "귀가" 기본 모드 1개 찾기
    Optional<Mode> findFirstByHo_IdAndIsDefaultTrueAndModeNameOrderByIdAsc(Long hoId, String modeName);
}