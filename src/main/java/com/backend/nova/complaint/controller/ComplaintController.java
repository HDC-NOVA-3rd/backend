package com.backend.nova.complaint.controller;

import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.complaint.dto.*;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Complaint", description = "민원 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/complaint")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    /* ================= 민원 등록 (입주민) ================= */
    @Operation(summary = "민원 등록", description = "입주민이 민원을 등록합니다.")
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping
    public ResponseEntity<Void> createComplaint(
            @AuthenticationPrincipal @Parameter(hidden = true) MemberDetails memberDetails,
            @RequestBody ComplaintCreateRequest request) {

        complaintService.createComplaint(memberDetails.getMemberId(), request);
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 수정 (입주민) ================= */
    @Operation(summary = "민원 정보 수정", description = "입주민이 본인의 민원을 수정합니다.")
    @PreAuthorize("hasRole('MEMBER')")
    @PutMapping("/{complaintId}")
    public ResponseEntity<Void> updateComplaint(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal @Parameter(hidden = true) MemberDetails memberDetails,
            @RequestBody ComplaintUpdateRequest request) {

        complaintService.updateComplaint(
                complaintId,
                memberDetails.getMemberId(),
                request
        );
        return ResponseEntity.ok().build();
    }





    /* ================= 민원 피드백 (입주민) ================= */
    @Operation(summary = "민원 피드백 등록", description = "입주민이 민원 처리에 대한 피드백을 남깁니다.")
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/{complaintId}/review")
    public ResponseEntity<Void> createFeedback(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal @Parameter(hidden = true) MemberDetails memberDetails,
            @RequestBody ComplaintReviewCreateRequest request) {

        complaintService.createReview(
                complaintId,
                memberDetails.getMemberId(),
                request
        );
        return ResponseEntity.ok().build();
    }



    /* ================= 내 민원 목록 조회 (입주민) ================= */
    @Operation(summary = "내 민원 목록 조회", description = "로그인한 입주민의 민원 목록을 조회합니다.")
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/list/member")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
            @AuthenticationPrincipal MemberDetails memberDetails) {

        List<ComplaintResponse> complaints =
                complaintService.getComplaintsByMember(memberDetails.getMemberId());
        return ResponseEntity.ok(complaints);
    }

    // ================= 민원 상세 조회 =================
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/{complaintId}/member")
    public ResponseEntity<ComplaintDetailResponse> getComplaintByMember(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal MemberDetails member) {

        ComplaintDetailResponse response = complaintService.getComplaintDetail(complaintId); // 타입 수정

        if (!response.memberId().equals(member.getMemberId())) {
            throw new AccessDeniedException("본인 민원만 조회 가능");
        }

        return ResponseEntity.ok(response);
    }

    /* ================= 민원 삭제 (입주민) ================= */
    @Operation(summary = "민원 삭제", description = "입주민이 본인의 민원을 삭제합니다.")
    @PreAuthorize("hasRole('MEMBER')")
    @DeleteMapping("/{complaintId}")
    public ResponseEntity<Void> deleteComplaint(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal @Parameter(hidden = true) MemberDetails memberDetails
    ) {
        complaintService.deleteComplaint(
                complaintId,
                memberDetails.getMemberId()
        );
        return ResponseEntity.noContent().build();
    }

    /* ================= 관리자 배정 (관리자) ================= */
    @Operation(summary = "관리자 배정", description = "민원에 담당 관리자를 배정합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{complaintId}/assign")
    public ResponseEntity<Void> assignAdmin(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal @Parameter(hidden = true) AdminDetails adminDetails,
            @RequestParam Long targetAdminId) {

        complaintService.assignAdmin(
                complaintId,
                adminDetails.getAdminId(),
                targetAdminId
        );
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 상태 변경 (관리자) ================= */
    @Operation(summary = "민원 상태 변경", description = "관리자가 민원 상태를 변경합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{complaintId}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal @Parameter(hidden = true) AdminDetails adminDetails,
            @RequestParam ComplaintStatus status) {

        complaintService.changeStatusByAdmin(
                complaintId,
                adminDetails.getAdminId(),
                status
        );
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 답변 등록 (관리자) ================= */
    @Operation(summary = "민원 답변 등록", description = "관리자가 민원에 답변을 등록합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{complaintId}/answers")
    public ResponseEntity<Void> createAnswer(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal @Parameter(hidden = true) AdminDetails adminDetails,
            @RequestBody ComplaintAnswerCreateRequest request) {

        complaintService.createAnswer(
                complaintId,
                adminDetails.getAdminId(),
                request
        );
        return ResponseEntity.ok().build();
    }

    /* ================= 민원 해결 완료 (관리자) ================= */
    @Operation(summary = "민원 해결 완료", description = "관리자가 민원을 해결 완료 처리합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{complaintId}/complete")
    public ResponseEntity<Void> completeComplaint(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal @Parameter(hidden = true) AdminDetails adminDetails) {

        complaintService.completeComplaint(
                complaintId,
                adminDetails.getAdminId()
        );
        return ResponseEntity.ok().build();
    }

    /* ================= 아파트별 민원 목록 조회 (관리자) ================= */
    @Operation(summary = "아파트별 민원 목록 조회", description = "관리자가 아파트 단지별 민원 목록을 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("list/apartment")
    public ResponseEntity<List<ComplaintResponse>> getComplaintsByAdminApartment(
            @AuthenticationPrincipal AdminDetails adminDetails) {

        Long apartmentId = adminDetails.getApartmentId();

        return ResponseEntity.ok(
                complaintService.getComplaintsByApartment(apartmentId)
        );
    }



    // ================= 관리자 민원 상세 조회 =================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{complaintId}/apartment")
    public ResponseEntity<ComplaintDetailResponse> getComplaintByAdmin(
            @PathVariable Long complaintId,
            @AuthenticationPrincipal AdminDetails admin) {

        ComplaintDetailResponse response = complaintService.getComplaintDetail(complaintId);

        if (!response.apartmentId().equals(admin.getApartmentId())) {
            throw new AccessDeniedException("관리 아파트 민원만 조회 가능");
        }

        return ResponseEntity.ok(response);
    }


    @Operation(summary = "삭제된 민원 조회 (일반, 슈퍼 관리자)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list/deleted")
    public ResponseEntity<List<ComplaintResponse>> getDeletedComplaints(
            @AuthenticationPrincipal AdminDetails adminDetails) {

        return ResponseEntity.ok(
                complaintService.getDeletedComplaints(adminDetails)
        );
    }




}
