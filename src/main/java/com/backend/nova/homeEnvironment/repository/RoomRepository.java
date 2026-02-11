package com.backend.nova.homeEnvironment.repository;

import com.backend.nova.homeEnvironment.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>{

    List<Room> findAllByHo_Id(Long hoId); //현재 방 리스트
    Optional<Object> findByHo_IdAndName(Long id, String roomName);
    boolean existsByHo_Id(Long hoId);
}
