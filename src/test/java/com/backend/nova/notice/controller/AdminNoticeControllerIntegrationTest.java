package com.backend.nova.notice.controller;

import com.backend.nova.ControllerTestSupport;
import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.notice.dto.NoticeCreateRequest;
import com.backend.nova.notice.dto.NoticeCreateResponse;
import com.backend.nova.notice.service.NoticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminNoticeController.class)
class AdminNoticeControllerTest extends ControllerTestSupport {

    @MockitoBean
    private NoticeService noticeService;

    @Test
    @DisplayName("관리자 공지 등록 성공")
    void createNotice_success() throws Exception {
        // given
        NoticeCreateRequest req = new NoticeCreateRequest("정기 소독 안내", "다음주 월요일 소독 예정", null);
        given(noticeService.createNotice(any(NoticeCreateRequest.class)))
                .willReturn(new NoticeCreateResponse(true, 1L));

        // when & then
        mockMvc.perform(post("/api/admin/notice")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.noticeId").value(1L));
    }

    private RequestPostProcessor adminAuth() {
        Apartment apartment = Apartment.builder()
                .id(1L)
                .name("테스트 아파트")
                .address("서울시 테스트구")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();
        
        Admin admin = Admin.builder()
                .loginId("test-admin")
                .email("admin@test.com")
                .password("test")
                .name("관리자")
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .failedLoginCount(0)
                .apartment(apartment)
                .build();
        
        AdminDetails adminDetails = new AdminDetails(admin);
        return authentication(
            new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities())
        );
    }
}
