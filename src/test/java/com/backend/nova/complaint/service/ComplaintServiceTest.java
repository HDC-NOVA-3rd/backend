package com.backend.nova.complaint.service;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.complaint.dto.ComplaintAnswerCreateRequest;
import com.backend.nova.complaint.dto.ComplaintCreateRequest;
import com.backend.nova.complaint.dto.ComplaintFeedbackCreateRequest;
import com.backend.nova.complaint.entity.Complaint;
import com.backend.nova.complaint.entity.ComplaintFeedback;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.repository.ComplaintAnswerRepository;
import com.backend.nova.complaint.repository.ComplaintFeedbackRepository;
import com.backend.nova.complaint.repository.ComplaintRepository;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.admin.repository.AdminRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.backend.nova.complaint.entity.ComplaintType.MAINTENANCE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock AdminRepository adminRepository;
    @Mock ComplaintRepository complaintRepository;
    @Mock ComplaintAnswerRepository complaintAnswerRepository;
    @Mock ComplaintFeedbackRepository complaintFeedbackRepository;

    @InjectMocks ComplaintService complaintService;

    @Test
    @DisplayName("민원 생성 성공")
    void createComplaint_success() {
        Member member = Member.builder().id(1L).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        complaintService.createComplaint(1L,
                new ComplaintCreateRequest(MAINTENANCE,"시설", "수도 고장"));

        verify(complaintRepository).save(any(Complaint.class));
    }

    @Test
    @DisplayName("관리자 상태 변경 - 권한 없음")
    void changeStatus_fail_noPermission() {
        Complaint complaint = Complaint.builder()
                .id(1L)
                .status(ComplaintStatus.ASSIGNED)
                .admin(Admin.builder().id(2L).build())
                .build();

        Admin admin = Admin.builder()
                .id(1L)
                .role(AdminRole.ADMIN)
                .build();

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() ->
                complaintService.changeStatusByAdmin(
                        1L, 1L, ComplaintStatus.IN_PROGRESS))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("민원 답변 등록 실패 - 완료된 민원")
    void createAnswer_fail_completedComplaint() {
        // given
        Complaint complaint = Complaint.builder()
                .id(1L)
                .status(ComplaintStatus.COMPLETED)
                .admin(Admin.builder().id(1L).build())
                .build();

        Admin admin = Admin.builder()
                .id(1L)
                .role(AdminRole.ADMIN)
                .build();

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        // when & then
        assertThatThrownBy(() ->
                complaintService.createAnswer(
                        1L,
                        1L,
                        new ComplaintAnswerCreateRequest("처리 완료했습니다.")
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("완료된 민원");

        verify(complaintAnswerRepository, never()).save(any());
    }

    @Test
    @DisplayName("민원 피드백 등록 실패 - 이미 피드백 존재")
    void createFeedback_fail_duplicate() {
        // given
        Complaint complaint = Complaint.builder()
                .id(1L)
                .status(ComplaintStatus.COMPLETED)
                .build();

        Member member = Member.builder()
                .id(10L)
                .build();

        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));
        when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
        when(complaintFeedbackRepository.findByComplaint_Id(1L))
                .thenReturn(Optional.of(mock(ComplaintFeedback.class)));

        // when & then
        assertThatThrownBy(() ->
                complaintService.createFeedback(
                        1L,
                        10L,
                        new ComplaintFeedbackCreateRequest("별로에요", new BigDecimal("1.50"))
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 피드백");

        verify(complaintFeedbackRepository, never()).save(any());
    }

}