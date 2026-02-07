package com.backend.nova.notice.controller;

import com.backend.nova.notice.dto.NoticeBoardResponse;
import com.backend.nova.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<List<NoticeBoardResponse>> getMyNotices(
            @AuthenticationPrincipal User user
    ) {
        List<NoticeBoardResponse> notices = noticeService.getNoticesForMember(user.getUsername());
        return ResponseEntity.ok(notices);
    }
}
