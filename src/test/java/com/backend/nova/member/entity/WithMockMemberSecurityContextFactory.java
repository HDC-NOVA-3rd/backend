package com.backend.nova.member.entity;

import com.backend.nova.auth.member.MemberDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.util.ReflectionTestUtils;

public class WithMockMemberSecurityContextFactory implements WithSecurityContextFactory<WithMockMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // 1. Member 엔티티 가짜 생성 (Builder 패턴 가정)
        // 만약 Builder가 없다면 new Member() 후 Setter를 사용하세요.
        Member member = Member.builder()
                .loginId(annotation.loginId())
                .password(annotation.password())
                .name(annotation.name())
                // .role(Role.MEMBER) // 필요하다면 Role 추가
                .build();

        // 2. ID는 보통 DB에서 생성되므로 Setter가 없을 수 있음. 리플렉션으로 강제 주입.
        ReflectionTestUtils.setField(member, "id", annotation.memberId());

        // 3. MemberDetails 생성 (Member 객체와 아파트 ID 주입)
        MemberDetails principal = new MemberDetails(member, annotation.apartmentId());

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