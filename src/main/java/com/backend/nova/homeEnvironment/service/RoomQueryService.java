package com.backend.nova.homeEnvironment.service;

import com.backend.nova.homeEnvironment.dto.RoomListItemResponse;
import com.backend.nova.homeEnvironment.repository.RoomRepository;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomQueryService {

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository; // 추가 (member 패키지에 있는 레포)

    // hoId로 방 목록 조회
    public List<RoomListItemResponse> getRoomsByHo(Long hoId) {

        return roomRepository.findAllByHo_Id(hoId)
                .stream()
                .map(RoomListItemResponse::from)
                .toList();
    }

    // 로그인 아이디로 내 방 목록
    public List<RoomListItemResponse> getRoomsByLoginId(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음: " + loginId));

        if (member.getResident() == null) {
            throw new IllegalStateException("거주지(resident) 정보가 없습니다.");
        }

        Long hoId = member.getResident().getHo().getId();
        return getRoomsByHo(hoId); // 기존 로직 재사용
    }
}
