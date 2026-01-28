package com.backend.nova.homeEnvironment.controller;

import com.backend.nova.homeEnvironment.dto.RoomEnvironmentResponse;
import com.backend.nova.homeEnvironment.service.HomeEnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class HomeEnvironmentController {

    private final HomeEnvironmentService homeEnvironmentService;

    //특정 방 실내 조회
    @GetMapping("/room/{roomId}/environment")
    public RoomEnvironmentResponse getRoomEnvironment(@PathVariable Long roomId){
        return homeEnvironmentService.getRoomEnvironment(roomId);
    }

    //전체 방 실내 조회
    @GetMapping("/ho/{hoId}/environment")
    public List<RoomEnvironmentResponse> getRoomEnvironments(@PathVariable Long hoId){
        return homeEnvironmentService.getRoomEnvironments(hoId);
    }
}
