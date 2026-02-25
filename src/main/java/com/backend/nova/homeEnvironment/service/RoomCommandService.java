package com.backend.nova.homeEnvironment.service;

import com.backend.nova.homeEnvironment.entity.Room;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomCommandService {

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;

    private Long getHoIdByLoginId(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음: " + loginId));

        if (member.getResident() == null) {
            throw new IllegalStateException("거주지(resident) 정보가 없습니다.");
        }
        return member.getResident().getHo().getId();
    }

    @Transactional
    public void updateRoomVisibility(String loginId, Long roomId, boolean visible) {
        Long hoId = getHoIdByLoginId(loginId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방 없음 roomId=" + roomId));

        // ✅ 내 세대 방인지 검증
        if (room.getHo() == null || room.getHo().getId() == null || !room.getHo().getId().equals(hoId)) {
            throw new IllegalStateException("내 세대 방만 변경 가능합니다. roomId=" + roomId);
        }

        // ✅ Room 엔티티에 hide()/show() 메서드가 있으면 그거 쓰고,
        // 없으면 room.setIsVisible(...) 같은 setter가 필요함.
        if (visible) room.show();
        else room.hide();
    }
}