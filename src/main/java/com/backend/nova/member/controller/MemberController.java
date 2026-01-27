package com.backend.nova.member.controller;

import com.backend.nova.member.dto.LoginRequest;
import com.backend.nova.member.dto.SignupRequest;
import com.backend.nova.member.dto.TokenResponse;
import com.backend.nova.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Member", description = "회원 관리 API")
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "로그인", description = "로그인 아이디와 비밀번호로 로그인하여 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
        TokenResponse tokenResponse = memberService.login(loginRequest);
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다.")
    @PostMapping("/signup")
    public ResponseEntity<Void> registerMember(@RequestBody SignupRequest request) {
        Long memberId = memberService.registerMember(request);
        return ResponseEntity.created(URI.create("/api/member/" + memberId)).build();
    }
}
