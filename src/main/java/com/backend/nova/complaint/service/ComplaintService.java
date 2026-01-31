package com.backend.nova.complaint.service;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.repository.AdminRepository;
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
    public void createComplaint(Long memberId, ComplaintCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        Complaint complaint = Complaint.builder()
                .member(member)
                .type(request.type())
                .title(request.title())
                .content(request.content())
                .build();

        complaintRepository.save(complaint);
    }

    /* ================= 멤버가 민원 수정 ================= */
    public void updateComplaint(Long complaintId, Long memberId, ComplaintUpdateRequest request) {
        Complaint complaint = findComplaint(complaintId);

        if (!complaint.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("본인 민원만 수정 가능");
        }

        complaint.update(request.title(), request.content(), request.type());
    }

    /* ================= 멤버가 민원 삭제 ================= */
    public void deleteComplaint(Long complaintId, Long memberId) {
        Complaint complaint = findComplaint(complaintId);

        if (!complaint.getMember().getId().equals(memberId)) {
            throw new IllegalStateException("본인 민원만 삭제 가능");
        }

        complaintRepository.delete(complaint);
    }

    //공통 권한 체크
    private void validateAnswerPermission(Complaint complaint, Admin admin) {

        // 최고 관리자는 무조건 가능
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            return;
        }

        // 배정된 관리자만 가능
        if (complaint.getAdmin() == null ||
                !complaint.getAdmin().getId().equals(admin.getId())) {
            throw new IllegalStateException("배정된 담당자만 답변할 수 있습니다.");
        }
    }


    /* ================= 관리자 배정 / 재배정 ================= */
    public void assignAdmin(Long complaintId, Long adminId, Long targetAdminId) {

        Complaint complaint = findComplaint(complaintId);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        if (admin.getRole() != AdminRole.SUPER_ADMIN) {
            throw new IllegalStateException("담당자 배정/변경 권한 없음");
        }

        Admin targetAdmin = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new IllegalArgumentException("대상 관리자 없음"));

        complaint.assignAdmin(targetAdmin);
    }


    /* ================= 권한별 관리자가 민원 진행 상태 변경 ================= */
    public void changeStatusByAdmin(Long complaintId, Long adminId,
                                    ComplaintStatus nextStatus) {

        Complaint complaint = findComplaint(complaintId);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        // SUPER_ADMIN은 무조건 가능
        if (admin.getRole() != AdminRole.SUPER_ADMIN) {
            // 배정 담당자만 가능
            if (!admin.getId().equals(complaint.getAdmin().getId())) {
                throw new IllegalStateException("상태 변경 권한 없음");
            }
        }

        complaint.changeStatus(nextStatus);
    }



    /* ================= 관리자가 민원 답변 등록 ================= */
    public void createAnswer(Long complaintId, Long adminId, ComplaintAnswerCreateRequest request) {
        Complaint complaint = findComplaint(complaintId);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        // 권한 체크 추가
        validateAnswerPermission(complaint, admin);

        // 답변 등록 시 상태 검증
        if (complaint.getStatus() == ComplaintStatus.COMPLETED) {
            throw new IllegalStateException("완료된 민원에는 답변을 등록할 수 없습니다.");
        }

        if (complaint.getStatus() != ComplaintStatus.ASSIGNED
                && complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new IllegalStateException("답변 등록 불가한 상태입니다.");
        }

        ComplaintAnswer answer = ComplaintAnswer.builder()
                .complaint(complaint)
                .admin(admin)
                .resultContent(request.resultContent())
                .build();

        complaintAnswerRepository.save(answer);

        // 첫 답변일 경우만 상태 변경
        if (complaint.getStatus() == ComplaintStatus.ASSIGNED) {
            complaint.changeStatus(ComplaintStatus.IN_PROGRESS);
        }
    }



    /* ================= 관리자 민원 해결 완료 담당자 / 슈퍼 관리자만 가능 ================= */
    public void completeComplaint(Long complaintId, Long adminId) {
        Complaint complaint = findComplaint(complaintId);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        if (admin.getRole() != AdminRole.SUPER_ADMIN &&
                !admin.getId().equals(complaint.getAdmin().getId())) {
            throw new IllegalStateException("민원 완료 권한 없음");
        }

        if (complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 민원만 완료할 수 있습니다.");
        }


        complaint.changeStatus(ComplaintStatus.COMPLETED);
    }


    /* ================= 멤버가 피드백 등록 ================= */
    public void createFeedback(Long complaintId, Long memberId, ComplaintFeedbackCreateRequest request) {
        Complaint complaint = findComplaint(complaintId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        if (!complaint.getStatus().equals(ComplaintStatus.COMPLETED)) {
            throw new IllegalStateException("해결 완료된 민원만 피드백 가능");
        }

        if (complaintFeedbackRepository.findByComplaint_Id(complaintId).isPresent()) {
            throw new IllegalStateException("이미 피드백이 등록된 민원입니다.");
        }


        ComplaintFeedback feedback = ComplaintFeedback.builder()
                .complaint(complaint)
                .member(member)
                .content(request.content())
                .rating(request.rating())
                .build();

        complaintFeedbackRepository.save(feedback);
    }

    /* ================= 공통 민원 조회 ================= */
    private Complaint findComplaint(Long complaintId) {
        return complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("민원 없음"));
    }


    //민원 상세 조회
    public ComplaintResponse getComplaintDetail(Long complaintId) {
        Complaint complaint = complaintRepository.findWithAllById(complaintId);
        if (complaint == null) {
            throw new IllegalArgumentException("민원 없음");
        }
        return ComplaintResponse.from(complaint);
    }


    // 멤버 본인 민원 목록
    public List<ComplaintResponse> getComplaintsByMember(Long memberId) {
        return complaintRepository.findByMember_Id(memberId).stream()
                .map(ComplaintResponse::from)
                .toList();
    }

    // 관리자 전체 조회 (아파트 기준)
    public List<ComplaintResponse> getComplaintsByApartment(Long apartmentId) {
        return complaintRepository.findByMember_Resident_Ho_Dong_Apartment_Id(apartmentId)
                .stream()
                .map(ComplaintResponse::from)
                .toList();
    }

//    @Transactional
//    public void deleteComplaint(Long memberId) {
//        complaintRepository.deleteById(memberId);
//    }
//    @Transactional
//    public void deleteAllComplaints(Long hoId) {
//        if (!hoRepository.existsById(hoId)) {
//            throw new IllegalArgumentException("해당 호가 없습니다. id=" + hoId);
//        }
//        complaintRepository.deleteByHoId(hoId);
//    }

}
