package com.backend.nova.resident.controller;

import com.backend.nova.resident.dto.ResidentRequestDto;
import com.backend.nova.resident.dto.ResidentResponseDto;
import com.backend.nova.resident.service.ResidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

    @PostMapping
    public ResponseEntity<Long> createResident(@RequestBody ResidentRequestDto requestDto) {
        Long residentId = residentService.createResident(requestDto);
        return ResponseEntity.ok(residentId);
    }

    @GetMapping("/{residentId}")
    public ResponseEntity<ResidentResponseDto> getResident(@PathVariable Long residentId) {
        ResidentResponseDto resident = residentService.getResident(residentId);
        return ResponseEntity.ok(resident);
    }

    @GetMapping("/{apartmentId}")
    public ResponseEntity<List<ResidentResponseDto>> getAllResidents(@PathVariable Long apartmentId) {
        List<ResidentResponseDto> residents = residentService.getAllResidents(apartmentId);
        return ResponseEntity.ok(residents);
    }

    @PutMapping("/{residentId}")
    public ResponseEntity<Void> updateResident(@PathVariable Long residentId, @RequestBody ResidentRequestDto requestDto) {
        residentService.updateResident(residentId, requestDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{residentId}")
    public ResponseEntity<Void> deleteResident(@PathVariable Long residentId) {
        residentService.deleteResident(residentId);
        return ResponseEntity.ok().build();
    }
}
