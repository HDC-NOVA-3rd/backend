package com.backend.nova.member.controller;

import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.member.dto.ChangePWRequest;
import com.backend.nova.member.dto.MemberApartmentResponse;
import com.backend.nova.member.dto.MemberInfoResponse;
import com.backend.nova.member.dto.PushTokenRequest;
import com.backend.nova.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<MemberInfoResponse> getMyInfo(@AuthenticationPrincipal MemberDetails memberDetails) {
        return ResponseEntity.ok(memberService.getMemberInfo(memberDetails.getUsername()));
    }

    @Operation(summary = "내 아파트 정보 조회")
    @GetMapping("/apartment")
    public ResponseEntity<MemberApartmentResponse> getMyApartmentInfo(@AuthenticationPrincipal MemberDetails memberDetails) {
        return ResponseEntity.ok(memberService.getMemberApartmentInfo(memberDetails.getUsername()));
    }

    @Operation(summary = "비밀번호 변경 (로그인 후)")
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @RequestBody ChangePWRequest request) {
        memberService.changePassword(memberDetails.getUsername(), request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "로그인 시 Expo 푸시 토큰 저장", description = "앱 실행/로그인 시 발급된 Expo Push Token을 DB에 저장합니다.")
    @PostMapping("/push-token")
    public ResponseEntity<Void> savePushToken(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @RequestBody PushTokenRequest request
    ) {
        memberService.savePushToken(memberDetails.getMemberId(), request.pushToken());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "로그아웃 시 Expo 푸시 토큰 삭제", description = "로그아웃 시 DB에 저장된 Push Token을 제거하여 알림이 가지 않도록 합니다.")
    @DeleteMapping("/push-token")
    public ResponseEntity<Void> deletePushToken(
            @AuthenticationPrincipal MemberDetails memberDetails,
            @RequestHeader("Authorization") String bearerToken
    ) {
        // 클라이언트가 보낸 "Bearer {토큰}" 문자열에서 순수 토큰 값만 추출
        String accessToken = bearerToken;
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            accessToken = bearerToken.substring(7);
        }
        memberService.logout(memberDetails.getMemberId(), memberDetails.getUsername(), accessToken);
        return ResponseEntity.ok().build();
    }
}