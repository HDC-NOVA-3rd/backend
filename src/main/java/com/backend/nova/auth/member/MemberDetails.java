package com.backend.nova.auth.member;
import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class MemberDetails extends User {

    private final Long memberId;
    private final String name;
    private final Long hoId;     // 세대 ID
    private final String hoNo;   // 세대 번호 (예: 101호)
    private final Integer floor; // 층수
    private final Long dongId;   // 동 ID

    public MemberDetails(Member member) {
        super(member.getLoginId(), member.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));

        this.memberId = member.getId();
        this.name = member.getName();

        // Resident → Ho → Dong
        Ho ho = member.getResident().getHo();
        this.hoId = ho.getId();
        this.hoNo = ho.getHoNo();
        this.floor = ho.getFloor();
        this.dongId = ho.getDong().getId();
    }
}