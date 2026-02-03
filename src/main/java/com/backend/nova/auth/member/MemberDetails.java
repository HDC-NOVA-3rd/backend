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
    private final Long hoId;
    private final String hoNo;
    private final Integer floor;
    private final Long dongId;

    public MemberDetails(Member member) {
        super(
                member.getLoginId(),
                member.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        this.memberId = member.getId();
        this.name = member.getName();

        Ho ho = member.getResident().getHo();
        this.hoId = ho.getId();
        this.hoNo = ho.getHoNo();
        this.floor = ho.getFloor();
        this.dongId = ho.getDong().getId();
    }
}
