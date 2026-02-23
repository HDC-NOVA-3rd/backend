package com.backend.nova.notice.service;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.auth.admin.AdminDetails;
import com.backend.nova.global.notification.NotificationService;
import com.backend.nova.global.notification.PushMessageRequest;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.notice.dto.NoticeBoardResponse;
import com.backend.nova.notice.dto.NoticeCreateRequest;
import com.backend.nova.notice.dto.NoticeCreateResponse;
import com.backend.nova.notice.dto.NoticeDetailResponse;
import com.backend.nova.notice.dto.NoticeLogResponse;
import com.backend.nova.notice.dto.NoticeSendRequest;
import com.backend.nova.notice.dto.NoticeSendResponse;
import com.backend.nova.notice.dto.NoticeUpdateRequest;
import com.backend.nova.notice.entity.Notice;
import com.backend.nova.notice.entity.NoticeSendLog;
import com.backend.nova.notice.entity.NoticeTargetDong;
import com.backend.nova.notice.entity.NoticeTargetScope;
import com.backend.nova.notice.repository.NoticeRepository;
import com.backend.nova.notice.repository.NoticeSendLogRepository;
import com.backend.nova.notice.repository.NoticeTargetDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeSendLogRepository noticeSendLogRepository;
    private final NoticeTargetDongRepository noticeTargetDongRepository;
    private final AdminRepository adminRepository;
    private final DongRepository dongRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    public NoticeCreateResponse createNotice(NoticeCreateRequest request) {
        Admin admin = getCurrentAdmin();
        NoticeTargetScope targetScope = resolveTargetScope(request);

        Notice notice = Notice.builder()
                .admin(admin)
                .title(request.title())
                .content(request.content())
                .targetScope(targetScope)
                .build();

        Notice saved = noticeRepository.save(notice);
        if (targetScope == NoticeTargetScope.DONG) {
            List<Dong> targets = resolveTargetDongs(request.dongIds(), admin);
            List<NoticeTargetDong> targetDongs = targets.stream()
                    .map(dong -> NoticeTargetDong.builder()
                            .notice(saved)
                            .dong(dong)
                            .build())
                    .toList();
            noticeTargetDongRepository.saveAll(targetDongs);
        }
        return new NoticeCreateResponse(true, saved.getId());
    }

    @Transactional(readOnly = true)
    public NoticeDetailResponse getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
        List<Long> dongIds = noticeTargetDongRepository.findDongIdsByNoticeId(noticeId);
        return NoticeDetailResponse.from(notice, dongIds);
    }

    public NoticeDetailResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        Admin admin = getCurrentAdmin();
        if (!notice.getAdmin().getId().equals(admin.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        notice.updateTitle(request.title());
        notice.updateContent(request.content());

        List<Long> normalizedDongIds = normalizeIds(request.dongIds());
        NoticeTargetScope newScope = normalizedDongIds.isEmpty()
                ? NoticeTargetScope.ALL : NoticeTargetScope.DONG;
        notice.updateTargetScope(newScope);

        noticeTargetDongRepository.deleteAllByNoticeId(noticeId);

        if (newScope == NoticeTargetScope.DONG) {
            List<Dong> targets = resolveTargetDongs(request.dongIds(), admin);
            List<NoticeTargetDong> targetDongs = targets.stream()
                    .map(dong -> NoticeTargetDong.builder()
                            .notice(notice)
                            .dong(dong)
                            .build())
                    .toList();
            noticeTargetDongRepository.saveAll(targetDongs);
        }

        List<Long> dongIds = noticeTargetDongRepository.findDongIdsByNoticeId(noticeId);
        return NoticeDetailResponse.from(notice, dongIds);
    }

    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        Admin admin = getCurrentAdmin();
        if (!notice.getAdmin().getId().equals(admin.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        noticeSendLogRepository.deleteAllByNoticeId(noticeId);
        noticeTargetDongRepository.deleteAllByNoticeId(noticeId);
        noticeRepository.delete(notice);
    }

    public NoticeSendResponse sendNoticeAlert(Long noticeId, NoticeSendRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        Admin admin = getCurrentAdmin();
        Set<Long> allowedDongIds = resolveAllowedDongIds(notice, admin);
        if (allowedDongIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<Member> targetMembers = resolveTargetMembers(request, admin, allowedDongIds);
        saveNoticeSendLogs(notice, targetMembers);

        List<PushMessageRequest> pushMessages = buildNoticePushMessages(notice, targetMembers);

        if (!pushMessages.isEmpty()) {
            notificationService.sendPushMessages(pushMessages);
        }
        return buildSendResponse(pushMessages.size());
    }

    @Transactional(readOnly = true)
    public List<NoticeLogResponse> getNoticeLogs() {
        return noticeSendLogRepository.findAllByOrderBySentAtDesc()
                .stream()
                .map(NoticeLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NoticeBoardResponse> getNoticesForMember(String loginId) {
        return memberRepository.findByLoginId(loginId)
                .map(this::getMemberBoardNotices)
                .orElseGet(() -> adminRepository.findByLoginId(loginId)
                        .map(this::getAdminBoardNotices)
                        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED)));
    }

    private List<NoticeBoardResponse> getMemberBoardNotices(Member member) {
        Long apartmentId = member.getResident().getHo().getDong().getApartment().getId();
        Long dongId = member.getResident().getHo().getDong().getId();
        return noticeRepository.findBoardNotices(apartmentId, dongId)
                .stream()
                .map(NoticeBoardResponse::from)
                .toList();
    }

    private List<NoticeBoardResponse> getAdminBoardNotices(Admin admin) {
        Long apartmentId = admin.getApartment().getId();
        return noticeRepository.findBoardNoticesForApartment(apartmentId)
                .stream()
                .map(NoticeBoardResponse::from)
                .toList();
    }

    private Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdminDetails adminDetails)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return adminRepository.findById(adminDetails.getAdminId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    private List<Member> resolveTargetMembers(NoticeSendRequest request, Admin admin, Set<Long> allowedDongIds) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Long apartmentId = admin.getApartment().getId();
        List<Long> requestedDongIds = normalizeIds(request.dongIds());
        List<Long> targetDongIds = requestedDongIds.isEmpty() ? List.copyOf(allowedDongIds) : requestedDongIds;
        validateDongIds(targetDongIds, apartmentId, allowedDongIds);

        List<Member> targetMembers = memberRepository.findByResident_Ho_Dong_IdIn(targetDongIds).stream()
                .filter(member -> member.getResident() != null && member.getResident().getId() != null)
                .toList();
        if (targetMembers.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return targetMembers;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ids;
    }

    private void validateDongIds(List<Long> dongIds, Long apartmentId, Set<Long> allowedDongIds) {
        List<Dong> dongs = dongRepository.findAllById(dongIds);
        validateRequest(dongs.size() == dongIds.size());
        boolean invalidDong = dongs.stream()
                .anyMatch(dong -> !dong.getApartment().getId().equals(apartmentId)
                        || !allowedDongIds.contains(dong.getId()));
        validateRequest(!invalidDong);
    }

    private void saveNoticeSendLogs(Notice notice, List<Member> targetMembers) {
        List<NoticeSendLog> logs = targetMembers.stream()
                .map(member -> NoticeSendLog.builder()
                        .notice(notice)
                        .recipientId(member.getResident().getId())
                        .title(notice.getTitle())
                        .content(notice.getContent())
                        .read(false)
                        .build())
                .toList();
        noticeSendLogRepository.saveAll(logs);
    }

    private List<PushMessageRequest> buildNoticePushMessages(
            Notice notice,
            List<Member> targetMembers
    ) {
        return targetMembers.stream()
                .map(member -> notificationService.sendNotification(
                        member.getPushToken(),
                        notice.getTitle(),
                        notice.getContent()))
                .filter(Objects::nonNull)
                .toList();
    }

    private NoticeSendResponse buildSendResponse(int sentCount) {
        String message = sentCount + "명에게 공지 알림이 전송되었습니다.";
        return new NoticeSendResponse(true, message, sentCount);
    }

    private void validateRequest(boolean condition) {
        if (!condition) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private List<Dong> resolveTargetDongs(List<Long> dongIds, Admin admin) {
        List<Long> normalized = normalizeIds(dongIds);
        List<Dong> dongs = normalized.isEmpty()
                ? dongRepository.findAllByApartmentId(admin.getApartment().getId())
                : dongRepository.findAllById(normalized);

        if (dongs.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (!normalized.isEmpty() && dongs.size() != normalized.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Long apartmentId = admin.getApartment().getId();
        boolean mismatchApartment = dongs.stream()
                .anyMatch(dong -> !dong.getApartment().getId().equals(apartmentId));
        if (mismatchApartment) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        return dongs;
    }

    private NoticeTargetScope resolveTargetScope(NoticeCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<Long> dongIds = normalizeIds(request.dongIds());
        return dongIds.isEmpty() ? NoticeTargetScope.ALL : NoticeTargetScope.DONG;
    }

    private Set<Long> resolveAllowedDongIds(Notice notice, Admin admin) {
        List<Long> allowedIds = notice.getTargetScope() == NoticeTargetScope.ALL
                ? dongRepository.findAllByApartmentId(admin.getApartment().getId()).stream()
                        .map(Dong::getId)
                        .toList()
                : noticeTargetDongRepository.findDongIdsByNoticeId(notice.getId());
        return allowedIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
