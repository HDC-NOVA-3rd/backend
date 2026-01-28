package com.backend.nova.homeEnvironment.repository;

import com.backend.nova.homeEnvironment.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    // 특정 세대(hoId)의 방 목록
    List<Room> findAllByHo_Id(Long hoId);
}

