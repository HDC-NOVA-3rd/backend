package com.backend.nova.complaint.controller;

import com.backend.nova.complaint.dto.*;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.service.ComplaintService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaint")
@RequiredArgsConstructor
@Tag(name = "Complaint", description = "민원 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class ComplaintController {

    private final ComplaintService complaintService;

    /* ================= 민원 등록 (입주민) ================= */
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping
    public ResponseEntity<Void> createComplaint(
            Authentication authentication,
            @RequestBody ComplaintCreateRequest request
    ) {
        complaintService.createComplaint(authentication, request);
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 수정 (입주민) ================= */
    @PreAuthorize("hasRole('MEMBER')")
    @PutMapping("/{complaintId}")
    public ResponseEntity<Void> updateComplaint(
            @PathVariable Long complaintId,
            Authentication authentication,
            @RequestBody ComplaintUpdateRequest request
    ) {
        complaintService.updateComplaint(complaintId, authentication, request);
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 삭제 (입주민) ================= */
    @PreAuthorize("hasRole('MEMBER')")
    @DeleteMapping("/{complaintId}")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long complaintId,
            Authentication authentication
    ) {
        complaintService.deleteComplaint(complaintId, authentication);
        return ResponseEntity.noContent().build();
    }

    /* ================= 관리자 배정 ================= */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{complaintId}/assign")
    public ResponseEntity<Void> assignAdmin(
            @PathVariable Long complaintId,
            Authentication authentication,
            @RequestParam Long targetAdminId
    ) {
        complaintService.assignAdmin(complaintId, authentication, targetAdminId);
        return ResponseEntity.ok().build();
    }

    /* ================= 상태 변경 ================= */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{complaintId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long complaintId,
            Authentication authentication,
            @RequestParam ComplaintStatus status
    ) {
        complaintService.changeStatusByAdmin(complaintId, authentication, status);
        return ResponseEntity.ok().build();
    }

    /* ================= 답변 등록 ================= */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{complaintId}/answers")
    public ResponseEntity<Void> createAnswer(
            @PathVariable Long complaintId,
            Authentication authentication,
            @RequestBody ComplaintAnswerCreateRequest request
    ) {
        complaintService.createAnswer(complaintId, authentication, request);
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 완료 ================= */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{complaintId}/complete")
    public ResponseEntity<Void> completeComplaint(
            @PathVariable Long complaintId,
            Authentication authentication
    ) {
        complaintService.completeComplaint(complaintId, authentication);
        return ResponseEntity.ok().build();
    }

    /* ================= 피드백 ================= */
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/{complaintId}/feedbacks")
    public ResponseEntity<Void> createFeedback(
            @PathVariable Long complaintId,
            Authentication authentication,
            @RequestBody ComplaintFeedbackCreateRequest request
    ) {
        complaintService.createFeedback(complaintId, authentication, request);
        return ResponseEntity.ok().build();
    }

    /* ================= 목록 ================= */
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/list/member")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(Authentication authentication) {
        return ResponseEntity.ok(complaintService.getComplaintsByMember(authentication));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list/apartment")
    public ResponseEntity<List<ComplaintResponse>> getComplaintsByApartment(Authentication authentication) {
        return ResponseEntity.ok(complaintService.getComplaintsByApartment(authentication));
    }

    /* ================= 상세 ================= */
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/{complaintId}/member")
    public ResponseEntity<ComplaintResponse> getComplaintByMember(
            @PathVariable Long complaintId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                complaintService.getComplaintDetailForMember(complaintId, authentication)
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{complaintId}/apartment")
    public ResponseEntity<ComplaintResponse> getComplaintByAdmin(
            @PathVariable Long complaintId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                complaintService.getComplaintDetailForAdmin(complaintId, authentication)
        );
    }

    /* ================= 삭제 민원 조회 ================= */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list/deleted")
    public ResponseEntity<List<ComplaintResponse>> getDeletedComplaints(Authentication authentication) {
        return ResponseEntity.ok(complaintService.getDeletedComplaints(authentication));
    }
}

