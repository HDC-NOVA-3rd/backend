package com.backend.nova.auth.member;
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
    private final Long apartmentId;
    private final Long hoId;

    public MemberDetails(Member member, Long apartmentId, Long hoId) {
        // 부모(User) 생성자 호출: (아이디, 비밀번호, 권한리스트)
        super(member.getLoginId(), member.getPassword(), List.of(new SimpleGrantedAuthority("MEMBER")));

        // 추가 정보 초기화
        this.memberId = member.getId();
        this.name = member.getName();
        this.apartmentId = apartmentId;
        this.hoId = hoId;
    }

    // Redis 캐시 데이터 기반 생성자 (DB 접근 X)
    public MemberDetails(Long memberId, String loginId, String name, Long apartmentId, Long hoId, String role) {
        super(loginId, "", List.of(new SimpleGrantedAuthority(role))); // 비밀번호는 불필요하므로 빈문자열
        this.memberId = memberId;
        this.name = name;
        this.apartmentId = apartmentId;
        this.hoId = hoId;
    }
}