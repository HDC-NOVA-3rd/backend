package com.backend.nova.member.controller;

import com.backend.nova.member.dto.*;
import com.backend.nova.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 및 토큰 관리 API (로그인, 가입, 재발급)")
@RestController
@RequestMapping("/api/member") // URL 유지
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @Operation(summary = "OAuth 인증 코드 교환", description = "Redirect URL로 받은 Code를 실제 토큰(로그인) 또는 가입정보(회원가입)로 교환합니다.")
    @PostMapping("/oauth/exchange")
    public ResponseEntity<AuthExchangeResponse> exchangeAuthCode(@RequestBody AuthCodeRequest request) {
        return ResponseEntity.ok(memberService.exchangeAuthCode(request.code()));
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(memberService.login(loginRequest));
    }

    @Operation(summary = "회원 가입")
    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> registerMember(@RequestBody SignupRequest request) {
        return ResponseEntity.ok(memberService.registerMember(request));
    }

    @Operation(summary = "Access 토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(memberService.refresh(request));
    }

    @Operation(summary = "아이디 찾기")
    @PostMapping("/findInfo")
    public ResponseEntity<FindIdResponse> findMemberId(@RequestBody FindIdRequest request) {
        return ResponseEntity.ok(memberService.findMemberId(request));
    }

    @Operation(summary = "비밀번호 재설정 (로그인 전)")
    @PostMapping("/resetPW")
    public ResponseEntity<ResetPWResponse> resetPassword(@RequestBody ResetPWRequest request) {
        return ResponseEntity.ok(memberService.resetPassword(request));
    }
}