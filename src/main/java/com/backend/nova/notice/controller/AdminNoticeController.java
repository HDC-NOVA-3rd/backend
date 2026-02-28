package com.backend.nova.notice.controller;

import com.backend.nova.notice.dto.NoticeCreateRequest;
import com.backend.nova.notice.dto.NoticeCreateResponse;
import com.backend.nova.notice.dto.NoticeDetailResponse;
import com.backend.nova.notice.dto.NoticeLogResponse;
import com.backend.nova.notice.dto.NoticeSendRequest;
import com.backend.nova.notice.dto.NoticeSendResponse;
import com.backend.nova.notice.dto.NoticeUpdateRequest;
import com.backend.nova.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notice")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminNoticeController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<NoticeCreateResponse> createNotice(
            @RequestBody @Valid NoticeCreateRequest request
    ) {
        NoticeCreateResponse response = noticeService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResponse> getNotice(@PathVariable Long noticeId) {
        NoticeDetailResponse response = noticeService.getNoticeDetail(noticeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResponse> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody @Valid NoticeUpdateRequest request
    ) {
        NoticeDetailResponse response = noticeService.updateNotice(noticeId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
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
