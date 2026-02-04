package com.backend.nova.auth.member;
import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.member.entity.Member;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class MemberDetails extends User {

    // 인증 객체에 담을 추가 정보
    private final Long memberId;
    private final String name;
    private final Long hoId;
    private final String hoNo;
    private final Integer floor;
    private final Long dongId;

    public MemberDetails(Member member) {
        // 부모(User) 생성자 호출: (아이디, 비밀번호, 권한리스트)
        super(member.getLoginId(), member.getPassword(), List.of(new SimpleGrantedAuthority("MEMBER")));
        super(
                member.getLoginId(),
                member.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        // 추가 정보 초기화
        this.memberId = member.getId();
        this.name = member.getName();

        Ho ho = member.getResident().getHo();
        this.hoId = ho.getId();
        this.hoNo = ho.getHoNo();
        this.floor = ho.getFloor();
        this.dongId = ho.getDong().getId();
    }
}
