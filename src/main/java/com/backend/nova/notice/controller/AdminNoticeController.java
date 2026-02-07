package com.backend.nova.notice.controller;

import com.backend.nova.notice.dto.NoticeCreateRequest;
import com.backend.nova.notice.dto.NoticeCreateResponse;
import com.backend.nova.notice.dto.NoticeLogResponse;
import com.backend.nova.notice.dto.NoticeSendRequest;
import com.backend.nova.notice.dto.NoticeSendResponse;
import com.backend.nova.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notice")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNoticeController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<NoticeCreateResponse> createNotice(
            @RequestBody @Valid NoticeCreateRequest request
    ) {
        NoticeCreateResponse response = noticeService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{noticeId}/send-alert")
    public ResponseEntity<NoticeSendResponse> sendNoticeAlert(
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeSendRequest request
    ) {
        NoticeSendResponse response = noticeService.sendNoticeAlert(noticeId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/log")
    public ResponseEntity<List<NoticeLogResponse>> getLogs() {
        List<NoticeLogResponse> logs = noticeService.getNoticeLogs();
        return ResponseEntity.ok(logs);
    }
}
