package com.backend.nova.complaint.controller;

import com.backend.nova.complaint.dto.*;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Complaint", description = "민원 관리 API")
@RestController
@RequestMapping("/api/complaint")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    /* ================= 민원 등록 (입주민) ================= */
    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> createComplaint(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ComplaintCreateRequest request) {

        complaintService.createComplaint(memberId, request);
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 수정 (입주민) ================= */
    @Operation(summary = "민원 정보 수정", description = "민원 정보를 수정합니다.")
    @PutMapping("/{complaintId}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> updateComplaint(
            @PathVariable("complaintId") Long complaintId,
            @AuthenticationPrincipal Long memberId,
            @RequestBody ComplaintUpdateRequest request) {

        complaintService.updateComplaint(complaintId, memberId, request);
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 삭제 (입주민) ================= */
    @Operation(summary = "민원 삭제", description = "민원 ID로 민원을 삭제합니다.")
    @DeleteMapping("/{complaintId}")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable("complaintId") Long complaintId,
            @AuthenticationPrincipal Long memberId) {

        complaintService.deleteComplaint(complaintId, memberId);
        return ResponseEntity.ok().build();
    }

    /* ================= 관리자 배정 (관리자) ================= */
    @PostMapping("/{complaintId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignAdmin(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal Long adminId,
            @RequestParam Long targetAdminId) {

        complaintService.assignAdmin(complaintId, adminId, targetAdminId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{complaintId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeStatus(
            @PathVariable("complaintId") Long complaintId,
            @AuthenticationPrincipal Long adminId,
            @RequestParam ComplaintStatus status) {

        complaintService.changeStatusByAdmin(complaintId, adminId, status);
        return ResponseEntity.ok().build();
    }


    /* ================= 민원 답변 등록 (관리자) ================= */
    @PostMapping("/{complaintId}/answers")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> createAnswer(
            @PathVariable("complaintId") Long complaintId,
            @AuthenticationPrincipal Long adminId,
            @RequestBody ComplaintAnswerCreateRequest request) {

        complaintService.createAnswer(complaintId, adminId, request);
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 해결 완료 (관리자) ================= */
    @PostMapping("/{complaintId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> completeComplaint(
            @PathVariable("complaintId") Long complaintId,
            @AuthenticationPrincipal Long adminId) {

        complaintService.completeComplaint(complaintId, adminId);
        return ResponseEntity.ok().build();
    }


    /* ================= 민원 피드백 (입주민) ================= */
    @PostMapping("/{complaintId}/feedbacks")
    @PreAuthorize("hasRole('MEMBER')")
    public ResponseEntity<Void> createFeedback(
            @PathVariable("complaintId") Long complaintId,
            @AuthenticationPrincipal Long memberId,
            @RequestBody ComplaintFeedbackCreateRequest request) {

        complaintService.createFeedback(complaintId, memberId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "민원 상세 조회", description = "민원 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{complaintId}")
    public ResponseEntity<ComplaintResponse> getComplaint(@PathVariable("complaintId") Long complaintId) {
        ComplaintResponse complaint = complaintService.getComplaintDetail(complaintId);
        return ResponseEntity.ok(complaint);
    }

    //사용자
    @Operation(summary = "사용자별 민원 목록 조회", description = "멤버 ID로 해당 아파트의 모든 민원을 조회합니다.")
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<ComplaintResponse>> getComplaintsByMember(@PathVariable Long memberId) {
        List<ComplaintResponse> complaints = complaintService.getComplaintsByMember(memberId);
        return ResponseEntity.ok(complaints);
    }

    //관리자
    @Operation(summary = "아파트별 민원 목록 조회", description = "아파트 ID로 해당 아파트의 모든 민원을 조회합니다.")
    @GetMapping("/apartment/{apartmentId}")
    public ResponseEntity<List<ComplaintResponse>> getComplaintsByApartment(@PathVariable Long apartmentId) {
        List<ComplaintResponse> complaints = complaintService.getComplaintsByApartment(apartmentId);
        return ResponseEntity.ok(complaints);
    }

//    //관리자
//    @Operation(summary = "단지별 민원 리스트 삭제", description = "아파트 단지 ID로 해당 세대의 민원을 모두 삭제합니다.")
//    @DeleteMapping("/apartment/{apartmentId}")
//    public ResponseEntity<Void> deleteAllComplaints(@PathVariable Long apartmentId) {
//        complaintService.deleteAllComplaints(apartmentId);
//        return ResponseEntity.ok().build();
//    }
//
//    //관리자
//    @Operation(summary = "호별 민원 리스트 삭제", description = "호 ID로 해당 세대의 민원을 모두 삭제합니다.")
//    @DeleteMapping("/ho/{hoId}")
//    public ResponseEntity<Void> deleteAllComplaints(@PathVariable Long hoId) {
//        complaintService.deleteAllComplaints(hoId);
//        return ResponseEntity.ok().build();
//    }
//
//    //사용자
//    @Operation(summary = "멤버별 민원 리스트 삭제", description = "멤버 ID로 해당 세대의 민원을 모두 삭제합니다.")
//    @DeleteMapping("/member/{memberId}")
//    public ResponseEntity<Void> deleteAllComplaints(@PathVariable Long memberId) {
//        complaintService.deleteAllComplaints(memberId);
//        return ResponseEntity.ok().build();
//    }
    

}