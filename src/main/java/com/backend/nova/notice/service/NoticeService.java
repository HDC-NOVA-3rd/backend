package com.backend.nova.notice.service;

import com.backend.nova.admin.entity.Admin;
import com.backend.nova.admin.repository.AdminRepository;
import com.backend.nova.apartment.entity.Dong;
import com.backend.nova.apartment.repository.DongRepository;
import com.backend.nova.global.exception.BusinessException;
import com.backend.nova.global.exception.ErrorCode;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.notice.dto.NoticeBoardResponse;
import com.backend.nova.notice.dto.NoticeCreateRequest;
import com.backend.nova.notice.dto.NoticeCreateResponse;
import com.backend.nova.notice.dto.NoticeLogResponse;
import com.backend.nova.notice.dto.NoticeSendRequest;
import com.backend.nova.notice.dto.NoticeSendResponse;
import com.backend.nova.notice.entity.Notice;
import com.backend.nova.notice.entity.NoticeSendLog;
import com.backend.nova.notice.entity.NoticeTargetDong;
import com.backend.nova.notice.entity.NoticeTargetScope;
import com.backend.nova.notice.repository.NoticeRepository;
import com.backend.nova.notice.repository.NoticeSendLogRepository;
import com.backend.nova.notice.repository.NoticeTargetDongRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
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
    private final ResidentRepository residentRepository;
    private final DongRepository dongRepository;
    private final MemberRepository memberRepository;

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

    public NoticeSendResponse sendNotice(Long noticeId, NoticeSendRequest request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        Admin admin = getCurrentAdmin();
        Set<Long> allowedDongIds = resolveAllowedDongIds(notice, admin);
        if (allowedDongIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Set<Long> targetResidentIds = resolveTargetResidentIds(request, admin, allowedDongIds);

        List<NoticeSendLog> logs = targetResidentIds.stream()
                .map(residentId -> NoticeSendLog.builder()
                        .notice(notice)
                        .recipientId(residentId)
                        .title(notice.getTitle())
                        .content(notice.getContent())
                        .read(false)
                        .build())
                .toList();

        noticeSendLogRepository.saveAll(logs);

        String message = targetResidentIds.size() + "명에게 공지가 전송되었습니다.";
        return new NoticeSendResponse(true, message, targetResidentIds.size());
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
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        Long apartmentId = member.getResident().getHo().getDong().getApartment().getId();
        Long dongId = member.getResident().getHo().getDong().getId();
        return noticeRepository.findBoardNotices(apartmentId, dongId)
                .stream()
                .map(NoticeBoardResponse::from)
                .toList();
    }

    private Admin getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long adminId;
        try {
            adminId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
    }

    private Set<Long> resolveTargetResidentIds(NoticeSendRequest request, Admin admin, Set<Long> allowedDongIds) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Set<Long> targetResidentIds = new LinkedHashSet<>();

        List<Long> residentIds = normalizeIds(request.residentIds());
        if (!residentIds.isEmpty()) {
            validateResidentIds(residentIds, admin, allowedDongIds);
            targetResidentIds.addAll(residentIds);
        }

        List<Long> dongIds = normalizeIds(request.dongIds());
        if (!dongIds.isEmpty()) {
            validateDongIds(dongIds, admin, allowedDongIds);
            List<Resident> residents = residentRepository.findByHo_Dong_IdIn(dongIds);
            if (residents.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            residents.stream()
                    .map(Resident::getId)
                    .filter(Objects::nonNull)
                    .forEach(targetResidentIds::add);
        }

        if (targetResidentIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return targetResidentIds;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return ids.stream().distinct().toList();
    }

    private void validateResidentIds(List<Long> residentIds, Admin admin, Set<Long> allowedDongIds) {
        List<Resident> residents = residentRepository.findAllById(residentIds);
        if (residents.size() != residentIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Long apartmentId = admin.getApartment().getId();
        boolean mismatchApartment = residents.stream()
                .anyMatch(resident -> !resident.getHo().getDong().getApartment().getId().equals(apartmentId));
        if (mismatchApartment) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!allowedDongIds.isEmpty()) {
            boolean notAllowed = residents.stream()
                    .anyMatch(resident -> !allowedDongIds.contains(resident.getHo().getDong().getId()));
            if (notAllowed) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }
    }

    private void validateDongIds(List<Long> dongIds, Admin admin, Set<Long> allowedDongIds) {
        List<Dong> dongs = dongRepository.findAllById(dongIds);
        if (dongs.size() != dongIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Long apartmentId = admin.getApartment().getId();
        boolean mismatchApartment = dongs.stream()
                .anyMatch(dong -> !dong.getApartment().getId().equals(apartmentId));
        if (mismatchApartment) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (!allowedDongIds.isEmpty() && !allowedDongIds.containsAll(dongIds)) {
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
        if (notice.getTargetScope() == NoticeTargetScope.ALL) {
            List<Dong> dongs = dongRepository.findAllByApartmentId(admin.getApartment().getId());
            return dongs.stream()
                    .map(Dong::getId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
        return new LinkedHashSet<>(noticeTargetDongRepository.findDongIdsByNoticeId(notice.getId()));
    }
}
