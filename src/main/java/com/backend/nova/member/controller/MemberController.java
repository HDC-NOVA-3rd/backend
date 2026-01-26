package com.backend.nova.member.controller;

import com.backend.nova.member.dto.MemberRequestDto;
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

    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다.")
    @PostMapping("/register")
    public ResponseEntity<Void> registerMember(@RequestBody MemberRequestDto requestDto) {
        Long memberId = memberService.registerMember(requestDto);
        return ResponseEntity.created(URI.create("/api/member/" + memberId)).build();
    }
}
