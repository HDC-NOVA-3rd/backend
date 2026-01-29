package com.backend.nova.homeEnvironment.repository;

import com.backend.nova.homeEnvironment.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long>{


    Optional<Object> findByHo_IdAndName(Long id, String roomName);
}
