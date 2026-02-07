package com.backend.nova.member.controller;

import com.backend.nova.member.dto.ChangePWRequest;
import com.backend.nova.member.dto.MemberApartmentResponse;
import com.backend.nova.member.dto.MemberInfoResponse;
import com.backend.nova.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "회원 정보 조회 및 수정 API (로그인 필요)")
@RestController
@RequestMapping("/api/member") // URL 유지
@SecurityRequirement(name = "bearerAuth") // 이 클래스의 모든 API에 자물쇠 🔒 적용
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/profile")
    public ResponseEntity<MemberInfoResponse> getMyInfo(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.getMemberInfo(user.getUsername()));
    }

    @Operation(summary = "내 아파트 정보 조회")
    @GetMapping("/apartment")
    public ResponseEntity<MemberApartmentResponse> getMyApartmentInfo(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(memberService.getMemberApartmentInfo(user.getUsername()));
    }

    @Operation(summary = "비밀번호 변경 (로그인 후)")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody ChangePWRequest request) {
        memberService.changePassword(user.getUsername(), request);
        return ResponseEntity.ok().build();
    }
}