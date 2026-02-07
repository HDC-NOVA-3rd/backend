package com.backend.nova.notice.service;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.notice.dto.NoticeCreateRequest;
import com.backend.nova.notice.dto.NoticeCreateResponse;
import com.backend.nova.notice.dto.NoticeSendRequest;
import com.backend.nova.notice.dto.NoticeSendResponse;
import com.backend.nova.notice.dto.NoticeBoardResponse;
import com.backend.nova.notice.entity.Notice;
import com.backend.nova.notice.entity.NoticeSendLog;
import com.backend.nova.notice.repository.NoticeRepository;
import com.backend.nova.notice.repository.NoticeSendLogRepository;
import com.backend.nova.notice.repository.NoticeTargetDongRepository;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    NoticeRepository noticeRepository;

    @Mock
    NoticeSendLogRepository noticeSendLogRepository;

    @Mock
    NoticeTargetDongRepository noticeTargetDongRepository;

    @Mock
    AdminRepository adminRepository;

    @Mock
    ResidentRepository residentRepository;

    @Mock
    DongRepository dongRepository;

    @Mock
    MemberRepository memberRepository;

    @InjectMocks
    NoticeService noticeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("공지 생성 - 성공")
    void createNotice_success() {
        setAdminAuthentication(1L);
        Apartment apartment = Apartment.builder()
                .id(1L)
                .name("Test Apt")
                .address("Address")
                .latitude(0.0)
                .longitude(0.0)
                .build();
        Admin admin = Admin.builder()
                .id(1L)
                .loginId("admin1")
                .password("pw")
                .name("Admin")
                .email("admin@test.com")
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .apartment(apartment)
                .build();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        com.backend.nova.apartment.entity.Dong dong = com.backend.nova.apartment.entity.Dong.builder()
                .id(10L)
                .apartment(apartment)
                .dongNo("101")
                .build();
        when(dongRepository.findAllById(List.of(10L))).thenReturn(List.of(dong));

        Notice saved = Notice.builder()
                .id(10L)
                .admin(admin)
                .title("Title")
                .content("Content")
                .targetScope(com.backend.nova.notice.entity.NoticeTargetScope.ALL)
                .build();
        when(noticeRepository.save(any(Notice.class))).thenReturn(saved);
        when(noticeTargetDongRepository.saveAll(any()))
                .thenReturn(List.of(mock(com.backend.nova.notice.entity.NoticeTargetDong.class)));

        NoticeCreateResponse response = noticeService.createNotice(new NoticeCreateRequest("Title", "Content", List.of(10L)));

        assertThat(response.success()).isTrue();
        assertThat(response.noticeId()).isEqualTo(10L);
        verify(noticeRepository).save(any(Notice.class));
    }

    @Test
    @DisplayName("공지 전송 - 동 ID 검증 실패")
    void sendNotice_invalidResidents() {
        setAdminAuthentication(1L);
        Admin admin = buildAdmin(1L);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        Notice notice = Notice.builder()
                .id(1L)
                .title("Title")
                .content("Content")
                .targetScope(com.backend.nova.notice.entity.NoticeTargetScope.DONG)
                .build();
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(noticeTargetDongRepository.findDongIdsByNoticeId(1L)).thenReturn(List.of(200L));
        when(dongRepository.findAllById(List.of(999L))).thenReturn(List.of());

        NoticeSendRequest request = new NoticeSendRequest(List.of(999L));

        assertThatThrownBy(() -> noticeService.sendNoticeAlert(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                });

        verify(noticeSendLogRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("공지 전송 - 성공")
    void sendNotice_success() {
        setAdminAuthentication(1L);
        Admin admin = buildAdmin(1L);
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        Notice notice = Notice.builder()
                .id(1L)
                .title("Title")
                .content("Content")
                .targetScope(com.backend.nova.notice.entity.NoticeTargetScope.DONG)
                .build();
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(noticeTargetDongRepository.findDongIdsByNoticeId(1L)).thenReturn(List.of(200L));
        Apartment apartment = admin.getApartment();
        com.backend.nova.apartment.entity.Dong dong = com.backend.nova.apartment.entity.Dong.builder()
                .id(200L)
                .apartment(apartment)
                .dongNo("101")
                .build();
        com.backend.nova.apartment.entity.Ho ho = com.backend.nova.apartment.entity.Ho.builder()
                .id(300L)
                .dong(dong)
                .hoNo("1001")
                .floor(10)
                .build();
        Resident resident1 = Resident.builder()
                .id(1L)
                .ho(ho)
                .name("김영희")
                .phone("010-0000-0001")
                .build();
        Resident resident2 = Resident.builder()
                .id(2L)
                .ho(ho)
                .name("이영희")
                .phone("010-0000-0002")
                .build();
        when(dongRepository.findAllById(List.of(200L))).thenReturn(List.of(dong));
        when(residentRepository.findByHo_Dong_IdIn(List.of(200L))).thenReturn(List.of(resident1, resident2));
        when(noticeSendLogRepository.saveAll(any()))
                .thenReturn(List.of(mock(NoticeSendLog.class), mock(NoticeSendLog.class)));

        NoticeSendResponse response = noticeService.sendNoticeAlert(1L, new NoticeSendRequest(List.of(200L)));

        assertThat(response.success()).isTrue();
        assertThat(response.sentCount()).isEqualTo(2);
        verify(noticeSendLogRepository).saveAll(any());
    }

    @Test
    @DisplayName("멤버 공지 조회 - 동 기준")
    void getNoticesForMember_success() {
        com.backend.nova.apartment.entity.Apartment apartment = Apartment.builder()
                .id(1L)
                .name("Test Apt")
                .address("Address")
                .latitude(0.0)
                .longitude(0.0)
                .build();
        com.backend.nova.apartment.entity.Dong dong = com.backend.nova.apartment.entity.Dong.builder()
                .id(200L)
                .apartment(apartment)
                .dongNo("101")
                .build();
        com.backend.nova.apartment.entity.Ho ho = com.backend.nova.apartment.entity.Ho.builder()
                .id(300L)
                .dong(dong)
                .hoNo("1001")
                .floor(10)
                .build();
        Resident resident = Resident.builder()
                .id(1L)
                .ho(ho)
                .name("김영희")
                .phone("010-0000-0001")
                .build();
        Member member = Member.builder()
                .id(5L)
                .resident(resident)
                .loginId("user1")
                .password("pw")
                .name("김영희")
                .loginType(LoginType.NORMAL)
                .build();
        when(memberRepository.findByLoginId("user1")).thenReturn(Optional.of(member));

        Notice notice = Notice.builder()
                .id(1L)
                .admin(buildAdmin(1L))
                .title("공지 제목")
                .content("공지 내용")
                .targetScope(com.backend.nova.notice.entity.NoticeTargetScope.ALL)
                .build();
        when(noticeRepository.findBoardNotices(1L, 200L))
                .thenReturn(List.of(notice));

        List<NoticeBoardResponse> responses = noticeService.getNoticesForMember("user1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("공지 제목");
    }

    private void setAdminAuthentication(Long adminId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                adminId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Admin buildAdmin(Long adminId) {
        Apartment apartment = Apartment.builder()
                .id(1L)
                .name("Test Apt")
                .address("Address")
                .latitude(0.0)
                .longitude(0.0)
                .build();
        return Admin.builder()
                .id(adminId)
                .loginId("admin1")
                .password("pw")
                .name("Admin")
                .email("admin@test.com")
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .apartment(apartment)
                .build();
    }
}
