package com.backend.nova.resident.controller;

import com.backend.nova.resident.service.ResidentService;
import com.backend.nova.config.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;


@WebMvcTest(ResidentController.class)
@Import(TestSecurityConfig.class)
class ResidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResidentService residentService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        ResidentService residentService() {
            return mock(ResidentService.class);
        }
    }

    @Test
    @DisplayName("입주민 인증 성공 테스트")
    void verifyResident_Success() throws Exception {
    }

    @Test
    @DisplayName("입주민 인증 실패 테스트")
    void verifyResident_Fail() throws Exception {
    }
}
