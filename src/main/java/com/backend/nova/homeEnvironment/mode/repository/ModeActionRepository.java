package com.backend.nova.homeEnvironment.mode.repository;

import com.backend.nova.homeEnvironment.mode.entity.ModeAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModeActionRepository extends JpaRepository<ModeAction, Long> {

    // 특정 모드(modeId)에 속한 액션들을 sortOrder 오름차순으로 조회 (화면 1,2,3 순서 보장)
    List<ModeAction> findAllByMode_IdOrderBySortOrderAsc(Long modeId);

    // 특정 모드(modeId)의 액션을 전부 삭제 (액션 교체/초기화/삭제 시 사용)
    void deleteAllByMode_Id(Long modeId);

    // 특정 모드(modeId)에 액션이 하나라도 있는지 확인 (비어있는 모드인지 체크)
    boolean existsByMode_Id(Long modeId);
}
