package com.backend.nova.complaint.service;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.complaint.dto.*;
import com.backend.nova.complaint.entity.Complaint;
import com.backend.nova.complaint.entity.ComplaintAnswer;
import com.backend.nova.complaint.entity.ComplaintReview;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.repository.ComplaintAnswerRepository;
import com.backend.nova.complaint.repository.ComplaintReviewRepository;
import com.backend.nova.complaint.repository.ComplaintRepository;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private final ComplaintReviewRepository complaintReviewRepository;

    /* ================= 멤버가 민원 등록 ================= */
    public void createComplaint(Long memberId, ComplaintCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));


        // 멤버의 아파트 조회
        Apartment apartment = member.getResident()
                .getHo()
                .getDong()
                .getApartment();

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
        if (complaint.getStatus() == ComplaintStatus.COMPLETED) {
            throw new IllegalStateException("완료된 민원은 삭제할 수 없습니다.");
        }

        complaint.softDelete();
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

        Admin targetAdmin = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new IllegalArgumentException("대상 관리자 없음"));

        // ─────────────────────────
        // 아파트 소속 체크
        // ─────────────────────────
        if (!complaint.getApartment().getId().equals(admin.getApartment().getId())) {
            throw new IllegalStateException("자기 아파트의 민원만 배정할 수 있습니다.");
        }

        // (선택) targetAdmin도 같은 아파트인지까지 체크하고 싶다면
        if (!complaint.getApartment().getId().equals(targetAdmin.getApartment().getId())) {
            throw new IllegalStateException("같은 아파트 관리자에게만 배정할 수 있습니다.");
        }

        // ─────────────────────────
        // 일반 관리자 제약
        // ─────────────────────────
        if (admin.getRole() == AdminRole.ADMIN) {

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
    public void changeStatusByAdmin(Long complaintId, Long adminId,
                                    ComplaintStatus nextStatus) {

        Complaint complaint = findComplaint(complaintId);
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        // SUPER_ADMIN은 무조건 가능
        if (admin.getRole() != AdminRole.SUPER_ADMIN) {
            // 배정 담당자만 가능
            if (complaint.getAdmin() == null ||
                    !admin.getId().equals(complaint.getAdmin().getId())) {
                throw new IllegalStateException("상태 변경 권한 없음");
            }
        }

        complaint.changeStatus(nextStatus);
    }



    /* ================= 관리자가 민원 답변 등록 ================= */
    public void createAnswer(Long complaintId, Long adminId, ComplaintAnswerCreateRequest request) {
        // 1 민원 재조회 (영속 상태 보장)
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalStateException("민원 조회 실패"));

        // 2 관리자 조회
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        // 3 권한 체크
        validateAnswerPermission(complaint, admin);

        // 4 상태 검증
        if (complaint.getStatus() == ComplaintStatus.COMPLETED) {
            throw new IllegalStateException("완료된 민원에는 답변을 등록할 수 없습니다.");
        }
        if (complaint.getStatus() != ComplaintStatus.ASSIGNED
                && complaint.getStatus() != ComplaintStatus.IN_PROGRESS) {
            throw new IllegalStateException("답변 등록 불가한 상태입니다.");
        }

        // 5 ComplaintAnswer 생성 (엔티티 Builder 사용)
        ComplaintAnswer answer = ComplaintAnswer.builder()
                .complaint(complaint)
                .admin(admin)
                .resultContent(request.resultContent())
                .build();

        // 6 저장
        complaintAnswerRepository.save(answer);

        // 7 첫 답변이면 상태 변경
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


    /* ================= 멤버가 리뷰 등록 ================= */
    public void createReview(Long complaintId, Long memberId, ComplaintReviewCreateRequest request) {
        Complaint complaint = findComplaint(complaintId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        if (!complaint.getStatus().equals(ComplaintStatus.COMPLETED)) {
            throw new IllegalStateException("해결 완료된 민원만 리뷰등록 가능");
        }

        if (complaintReviewRepository.findByComplaint_Id(complaintId).isPresent()) {
            throw new IllegalStateException("이미 리뷰가 등록된 민원입니다.");
        }

        if (complaintReviewRepository.existsByComplaintId(complaintId)) {
            throw new IllegalStateException("이미 리뷰가 등록된 민원입니다.");
        }


        ComplaintReview review = ComplaintReview.builder()
                .complaint(complaint)
                .member(member)
                .content(request.content())
                .rating(request.rating())
                .build();

        complaintReviewRepository.save(review);
    }

    /* ================= 공통 민원 조회 ================= */
    public Complaint findComplaint(Long id) {
        return complaintRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("민원 없음"));
    }


    //멤버 본인 민원 상세 조회
    @Transactional(readOnly = true)
    public ComplaintDetailResponse getComplaintDetail(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new EntityNotFoundException("민원을 찾을 수 없습니다."));

        // 1. 해당 민원에 대한 리뷰가 있는지 확인
        boolean hasReview = complaintReviewRepository.existsByComplaintId(complaintId);

        // 2. 답변 정보 조회 (Repository가 Optional<ComplaintAnswer>를 반환한다고 가정)
        ComplaintAnswerResponse answerDto = complaintAnswerRepository.findByComplaintId(complaintId)
                .map(ComplaintAnswerResponse::from)
                .orElse(null);

        return ComplaintDetailResponse.of(complaint, hasReview, answerDto);
    }




    // 멤버 본인 민원 목록
    public List<ComplaintResponse> getComplaintsByMember(Long memberId) {
        return complaintRepository.findByMember_IdAndDeletedFalse(memberId).stream()
                .map(ComplaintResponse::from)
                .toList();
    }

    // 관리자 전체 조회 (아파트 기준)
    public List<ComplaintResponse> getComplaintsByApartment(Long apartmentId) {
        return complaintRepository.findByMember_Resident_Ho_Dong_Apartment_IdAndDeletedFalse(apartmentId)
                .stream()
                .map(ComplaintResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getDeletedComplaints(AdminDetails adminDetails) {

        if (adminDetails.getRoleEnum() != AdminRole.SUPER_ADMIN) {
            throw new AccessDeniedException("슈퍼 관리자만 조회할 수 있습니다.");
        }

        Long apartmentId = adminDetails.getApartmentId();

        return complaintRepository.findByDeletedTrueAndApartment_Id(apartmentId).stream()
                .map(ComplaintResponse::from)
                .toList();
    }

}
