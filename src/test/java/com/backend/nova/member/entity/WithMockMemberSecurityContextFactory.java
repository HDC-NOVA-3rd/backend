package com.backend.nova.member.entity;

import com.backend.nova.apartment.entity.Ho;
import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.resident.entity.Resident;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.util.ReflectionTestUtils;

public class WithMockMemberSecurityContextFactory implements WithSecurityContextFactory<WithMockMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // 1. [추가됨] 가짜 Ho(호) 객체 생성 및 ID 주입
        // (Ho 엔티티에 @Builder가 없다면 new Ho() 후 ReflectionTestUtils 사용)
        Ho mockHo = Ho.builder().build();
        ReflectionTestUtils.setField(mockHo, "id", annotation.apartmentId());

        // 2. [추가됨] 가짜 Resident(입주민) 객체 생성 및 Ho 연결
        Resident mockResident = Resident.builder()
                .ho(mockHo)
                .build();

        // 1. Member 엔티티 가짜 생성 (Builder 패턴 가정)
        // 만약 Builder가 없다면 new Member() 후 Setter를 사용하세요.
        Member member = Member.builder()
                .loginId(annotation.loginId())
                .password(annotation.password())
                .name(annotation.name())
                .resident(mockResident)
                .build();

        // 2. ID는 보통 DB에서 생성되므로 Setter가 없을 수 있음. 리플렉션으로 강제 주입.
        ReflectionTestUtils.setField(member, "id", annotation.memberId());

        // 3. MemberDetails 생성 (Member 객체와 아파트 ID 주입)
        MemberDetails principal = new MemberDetails(member, annotation.apartmentId(), annotation.hoId());

        // 4. 인증 토큰 생성
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                principal,
                principal.getPassword(),
                principal.getAuthorities()
        );

        // 5. Context에 설정
        context.setAuthentication(token);
        return context;
    }
}