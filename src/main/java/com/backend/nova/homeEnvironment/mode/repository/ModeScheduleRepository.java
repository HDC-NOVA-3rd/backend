package com.backend.nova.homeEnvironment.mode.repository;

import com.backend.nova.homeEnvironment.mode.entity.ModeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModeScheduleRepository extends JpaRepository<ModeSchedule, Long> {

    // 특정 모드(modeId)의 스케줄 목록 조회 (예약 정보 화면 표시용)
    // 특정 모드 하나를 선택했을 때 그 모드에 걸린 예약 규칙들을 가져옴
    List<ModeSchedule> findAllByMode_Id(Long modeId);

    // 특정 모드(modeId)의 스케줄 중 하나(대표)를 가져오기 위해 "id 오름차순 첫번째" 조회
    ModeSchedule findFirstByMode_IdOrderByIdAsc(Long modeId);

    // 특정 모드(modeId)의 "활성화된" 스케줄만 조회 (스케줄러가 실행 대상만 뽑을 때 사용)
    List<ModeSchedule> findAllByMode_IdAndIsEnabledTrue(Long modeId);

    // 특정 모드(modeId)의 스케줄을 전부 삭제 (예약 해제(SCHEDULE_CLEAR)에서 사용)
    void deleteAllByMode_Id(Long modeId);

    // 활성화된 스케줄 전체 조회 (서버 스케줄러가 주기적으로 돌 때 전체 대상 가져오기)
    List<ModeSchedule> findAllByIsEnabledTrue();
}
