package com.backend.nova.apartment.controller;

import com.backend.nova.apartment.dto.ApartmentStructure.ApartmentResponse;
import com.backend.nova.apartment.dto.ApartmentStructure.DongResponse;
import com.backend.nova.apartment.dto.ApartmentStructure.HoResponse;
import com.backend.nova.apartment.service.AptStructureService;
import com.backend.nova.auth.admin.AdminAuthenticationProvider;
import com.backend.nova.auth.jwt.JwtProvider;
import com.backend.nova.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AptStructureController.class)
@Import(SecurityConfig.class) // Security 설정 로드
class AptStructureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AptStructureService aptStructureService;

    // SecurityConfig 로딩을 위해 필요한 빈들을 Mock 처리
    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AdminAuthenticationProvider adminAuthenticationProvider;

    @Test
    @DisplayName("아파트 목록 조회 성공 테스트")
    void getApartmentList_Success() throws Exception {
        // given
        List<ApartmentResponse> response = List.of(
                new ApartmentResponse(1L, "래미안 아파트"),
                new ApartmentResponse(2L, "자이 아파트")
        );
        given(aptStructureService.getApartmentList()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/apartment")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("래미안 아파트"))
                .andExpect(jsonPath("$[1].name").value("자이 아파트"));
    }

    @Test
    @DisplayName("특정 아파트의 동 목록 조회 성공 테스트")
    void getDongList_Success() throws Exception {
        // given
        Long apartmentId = 1L;
        List<DongResponse> response = List.of(
                new DongResponse(10L, "101동"),
                new DongResponse(11L, "102동")
        );
        given(aptStructureService.getDongListByApartmentId(apartmentId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/apartment/{apartmentId}/dong", apartmentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].dongNo").value("101동"))
                .andExpect(jsonPath("$[1].dongNo").value("102동"));
    }

    @Test
    @DisplayName("특정 동의 호 목록 조회 성공 테스트")
    void getHoList_Success() throws Exception {
        // given
        Long dongId = 10L;
        List<HoResponse> response = List.of(
                new HoResponse(100L, "101호"),
                new HoResponse(101L, "102호")
        );
        given(aptStructureService.getHoListByDongId(dongId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/apartment/dong/{dongId}/ho", dongId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].hoNo").value("101호"))
                .andExpect(jsonPath("$[1].hoNo").value("102호"));
    }
}