package com.backend.nova.complaint.service;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.complaint.dto.*;
import com.backend.nova.complaint.entity.Complaint;
import com.backend.nova.complaint.entity.ComplaintAnswer;
import com.backend.nova.complaint.entity.ComplaintFeedback;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.repository.ComplaintAnswerRepository;
import com.backend.nova.complaint.repository.ComplaintFeedbackRepository;
import com.backend.nova.complaint.repository.ComplaintRepository;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintService {

    private final MemberRepository memberRepository;
    private final AdminRepository adminRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintAnswerRepository complaintAnswerRepository;
    private final ComplaintFeedbackRepository complaintFeedbackRepository;

    /* ================= 멤버가 민원 등록 ================= */
    public void createComplaint(Authentication authentication, ComplaintCreateRequest request) {
        MemberDetails memberDetails = getMember(authentication);
        Member member = memberRepository.findById(memberDetails.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        // 멤버의 아파트 조회
        var apartment = member.getResident().getHo().getDong().getApartment();

        Complaint complaint = Complaint.builder()
                .member(member)
                .apartment(apartment)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .build();

        complaintRepository.save(complaint);
    }

    /* ================= 멤버가 민원 수정 ================= */
    public void updateComplaint(Long complaintId, Authentication authentication, ComplaintUpdateRequest request) {
        MemberDetails member = getMember(authentication);
        Complaint complaint = findComplaint(complaintId);

        if (!complaint.getMember().getId().equals(member.getMemberId())) {
            throw new AccessDeniedException("본인 민원만 수정 가능");
        }

        complaint.update(request.title(), request.content(), request.type());
    }

    /* ================= 멤버가 민원 삭제 ================= */
    public void deleteComplaint(Long complaintId, Authentication authentication) {
        MemberDetails member = getMember(authentication);
        Complaint complaint = findComplaint(complaintId);

        if (!complaint.getMember().getId().equals(member.getMemberId())) {
            throw new AccessDeniedException("본인 민원만 삭제 가능");
        }

        complaint.softDelete();
    }

    /* ================= 관리자 배정 / 재배정 ================= */
    public void assignAdmin(Long complaintId, Authentication authentication, Long targetAdminId) {
        Complaint complaint = findComplaint(complaintId);
        AdminDetails adminDetails = getAdmin(authentication);
        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));
        Admin targetAdmin = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new IllegalArgumentException("대상 관리자 없음"));

        // ─────────────────────────
        // 아파트 소속 체크
        // ─────────────────────────
        if (!complaint.getApartment().getId().equals(admin.getApartment().getId())) {
            throw new AccessDeniedException("자기 아파트의 민원만 배정할 수 있습니다.");
        }

        if (!complaint.getApartment().getId().equals(targetAdmin.getApartment().getId())) {
            throw new IllegalStateException("같은 아파트 관리자에게만 배정할 수 있습니다.");
        }

        // ─────────────────────────
        // 일반 관리자 제약
        // ─────────────────────────
        if (admin.getRole() == AdminRole.ADMIN) {
            if (complaint.getAdmin() != null) {
                throw new IllegalStateException("일반 관리자는 재배정할 수 없습니다.");
            }
            if (!admin.getId().equals(targetAdmin.getId())) {
                throw new IllegalStateException("본인에게만 배정할 수 있습니다.");
            }
        }

        // ─────────────────────────
        // 배정 / 재배정
        // ─────────────────────────
        if (complaint.getAdmin() == null) {
            complaint.assignAdmin(targetAdmin);
        } else {
            complaint.reassignAdmin(targetAdmin);
        }
    }

    /* ================= 권한별 관리자가 민원 진행 상태 변경 ================= */
    public void changeStatusByAdmin(Long complaintId, Authentication authentication, ComplaintStatus nextStatus) {
        Complaint complaint = findComplaint(complaintId);
        AdminDetails adminDetails = getAdmin(authentication);
        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        // SUPER_ADMIN은 무조건 가능
        if (admin.getRole() != AdminRole.SUPER_ADMIN) {
            // 배정 담당자만 가능
            if (complaint.getAdmin() == null || !admin.getId().equals(complaint.getAdmin().getId())) {
                throw new AccessDeniedException("상태 변경 권한 없음");
            }
        }

        complaint.changeStatus(nextStatus);
    }

    /* ================= 관리자가 민원 답변 등록 ================= */
    public void createAnswer(Long complaintId, Authentication authentication, ComplaintAnswerCreateRequest request) {
        Complaint complaint = findComplaint(complaintId);
        AdminDetails adminDetails = getAdmin(authentication);
        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        // 권한 체크
        validateAnswerPermission(complaint, admin);

        // 상태 검증
        if (complaint.getStatus() == ComplaintStatus.COMPLETED) {
            throw new IllegalStateException("완료된 민원에는 답변을 등록할 수 없습니다.");
        }
        if (complaint.getStatus() != ComplaintStatus.ASSIGNED && complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new IllegalStateException("답변 등록 불가한 상태입니다.");
        }

        ComplaintAnswer answer = ComplaintAnswer.builder()
                .complaint(complaint)
                .admin(admin)
                .resultContent(request.resultContent())
                .build();

        complaintAnswerRepository.save(answer);

        // 첫 답변이면 상태 변경
        if (complaint.getStatus() == ComplaintStatus.ASSIGNED) {
            complaint.changeStatus(ComplaintStatus.IN_PROGRESS);
        }
    }

    /* ================= 관리자 민원 해결 완료 ================= */
    public void completeComplaint(Long complaintId, Authentication authentication) {
        Complaint complaint = findComplaint(complaintId);
        AdminDetails adminDetails = getAdmin(authentication);
        Admin admin = adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        if (admin.getRole() != AdminRole.SUPER_ADMIN && !admin.getId().equals(complaint.getAdmin().getId())) {
            throw new AccessDeniedException("민원 완료 권한 없음");
        }

        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 민원만 완료할 수 있습니다.");
        }

        complaint.changeStatus(ComplaintStatus.COMPLETED);
    }

    /* ================= 멤버가 피드백 등록 ================= */
    public void createFeedback(Long complaintId, Authentication authentication, ComplaintFeedbackCreateRequest request) {
        Complaint complaint = findComplaint(complaintId);
        MemberDetails member = getMember(authentication);
        Member m = memberRepository.findById(member.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        if (!complaint.getStatus().equals(ComplaintStatus.COMPLETED)) {
            throw new IllegalStateException("해결 완료된 민원만 피드백 가능");
        }

        if (complaintFeedbackRepository.findByComplaint_Id(complaintId).isPresent()) {
            throw new IllegalStateException("이미 피드백이 등록된 민원입니다.");
        }

        ComplaintFeedback feedback = ComplaintFeedback.builder()
                .complaint(complaint)
                .member(m)
                .content(request.content())
                .rating(request.rating())
                .build();

        complaintFeedbackRepository.save(feedback);
    }

    /* ================= 공통 민원 조회 ================= */
    public Complaint findComplaint(Long id) {
        return complaintRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("민원 없음"));
    }

    /* ================= 멤버 민원 상세 조회 ================= */
    public ComplaintResponse getComplaintDetailForMember(Long complaintId, Authentication authentication) {
        MemberDetails member = getMember(authentication);
        Complaint complaint = findComplaint(complaintId);

        if (!complaint.getMember().getId().equals(member.getMemberId())) {
            throw new AccessDeniedException("본인 민원만 조회 가능");
        }

        return ComplaintResponse.from(complaint);
    }

    /* ================= 관리자 민원 상세 조회 ================= */
    public ComplaintResponse getComplaintDetailForAdmin(Long complaintId, Authentication authentication) {
        AdminDetails admin = getAdmin(authentication);
        Complaint complaint = findComplaint(complaintId);

        // 일반 관리자는 배정된 민원만 조회 가능
        if (admin.getRoleEnum() != AdminRole.SUPER_ADMIN &&
                (complaint.getAdmin() == null || !complaint.getAdmin().getId().equals(admin.getAdminId()))) {
            throw new AccessDeniedException("권한 없는 민원 조회");
        }

        return ComplaintResponse.from(complaint);
    }

    /* ================= 멤버 인증 헬퍼 ================= */
    private MemberDetails getMember(Authentication authentication) {
        if (authentication.getPrincipal() instanceof MemberDetails member) {
            return member;
        }
        throw new AccessDeniedException("입주민 권한 필요");
    }

    /* ================= 관리자 인증 헬퍼 ================= */
    private AdminDetails getAdmin(Authentication authentication) {
        if (authentication.getPrincipal() instanceof AdminDetails admin) {
            return admin;
        }
        throw new AccessDeniedException("관리자 권한 필요");
    }

    /* ================= 답변 권한 검증 ================= */
    private void validateAnswerPermission(Complaint complaint, Admin admin) {
        // 최고 관리자는 무조건 가능
        if (admin.getRole() == AdminRole.SUPER_ADMIN) return;

        // 배정된 관리자만 가능
        if (complaint.getAdmin() == null || !complaint.getAdmin().getId().equals(admin.getId())) {
            throw new AccessDeniedException("배정된 담당자만 답변할 수 있습니다.");
        }
    }

    /* ================= 멤버 본인 민원 목록 ================= */
    public List<ComplaintResponse> getComplaintsByMember(Authentication authentication) {
        MemberDetails member = getMember(authentication);
        return complaintRepository.findByMember_IdAndDeletedFalse(member.getMemberId()).stream()
                .map(ComplaintResponse::from)
                .toList();
    }

    /* ================= 관리자 전체 조회 (아파트 기준) ================= */
    public List<ComplaintResponse> getComplaintsByApartment(Authentication authentication) {
        AdminDetails admin = getAdmin(authentication);
        return complaintRepository.findByMember_Resident_Ho_Dong_Apartment_IdAndDeletedFalse(admin.getApartmentId())
                .stream()
                .map(ComplaintResponse::from)
                .toList();
    }

    /* ================= 삭제 민원 조회 (슈퍼 관리자만) ================= */
    public List<ComplaintResponse> getDeletedComplaints(Authentication authentication) {
        AdminDetails admin = getAdmin(authentication);

        if (admin.getRoleEnum() != AdminRole.SUPER_ADMIN) {
            throw new AccessDeniedException("슈퍼 관리자만 조회할 수 있습니다.");
        }

        return complaintRepository.findByDeletedTrueAndApartment_Id(admin.getApartmentId()).stream()
                .map(ComplaintResponse::from)
                .toList();
    }
}
