package com.backend.nova.complaint.controller;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.entity.AdminRole;
import com.backend.nova.admin.entity.AdminStatus;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Apartment;
import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.apartment.repository.ApartmentRepository;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.apartment.repository.HoRepository;
import com.backend.nova.complaint.dto.ComplaintCreateRequest;
import com.backend.nova.complaint.dto.ComplaintResponse;
import com.backend.nova.complaint.entity.ComplaintStatus;
import com.backend.nova.complaint.repository.ComplaintRepository;
import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.backend.nova.complaint.entity.ComplaintType.NOISE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=test-secret-key-test-secret-key"
})
class ComplaintControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired ApartmentRepository apartmentRepository;
    @Autowired DongRepository dongRepository;
    @Autowired HoRepository hoRepository;
    @Autowired ResidentRepository residentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired AdminRepository adminRepository;
    @Autowired ComplaintRepository complaintRepository;

    @BeforeEach
    void clean() {
        complaintRepository.deleteAll();
        memberRepository.deleteAll();
        residentRepository.deleteAll();
        hoRepository.deleteAll();
        dongRepository.deleteAll();
        adminRepository.deleteAll();
        apartmentRepository.deleteAll();
    }

    @Test
    @DisplayName("입주민 민원 등록 → 상세 조회 성공")
    void complaint_flow_success() throws Exception {
        Apartment apartment = apartmentRepository.save(Apartment.builder()
                .name("테스트아파트")
                .address("주소")
                .latitude(0.0)
                .longitude(0.0)
                .build());

        Dong dong = dongRepository.save(Dong.builder()
                .apartment(apartment)
                .dongNo("101")
                .build());

        Ho ho = hoRepository.save(Ho.builder()
                .dong(dong)
                .hoNo("1001")
                .floor(10)
                .build());

        Resident resident = residentRepository.save(Resident.builder()
                .ho(ho)
                .name("김영희")
                .phone("010-0000-0001")
                .build());

        Member member = memberRepository.save(Member.builder()
                .resident(resident)
                .loginId("user1")
                .password("pw")
                .name("김영희")
                .loginType(LoginType.NORMAL)
                .build());

        ComplaintCreateRequest request =
                new ComplaintCreateRequest(NOISE,"엘리베", "엘리베이터 고장");

        mockMvc.perform(post("/api/complaint")
                        .with(user(member.getId().toString()).roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Long complaintId = complaintRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/complaint/{id}", complaintId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("엘리베이터 고장"))
                .andExpect(jsonPath("$.status").value(ComplaintStatus.RECEIVED.name()));
    }
}
