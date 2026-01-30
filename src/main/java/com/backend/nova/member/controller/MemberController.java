package com.backend.nova.member.controller;

import com.backend.nova.member.dto.*;
import com.backend.nova.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원의 상세 정보를 조회합니다.")
    @GetMapping("/profile")
    public ResponseEntity<MemberInfoResponse> getMyInfo(@AuthenticationPrincipal User user) {
        MemberInfoResponse response = memberService.getMemberInfo(user.getUsername());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 아파트 정보 조회", description = "현재 로그인한 회원의 아파트(동/호수) 정보를 조회합니다.")
    @GetMapping("/apartment")
    public ResponseEntity<MemberApartmentResponse> getMyApartmentInfo(@AuthenticationPrincipal User user) {
        MemberApartmentResponse response = memberService.getMemberApartmentInfo(user.getUsername());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Access 토큰 재발급", description = "Access 토큰이 만료되는 경우 Refresh 토큰을 사용하여 새로운 Access 토큰을 발급받습니다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = memberService.refresh(request);
        return ResponseEntity.ok(tokenResponse);
    }
}
