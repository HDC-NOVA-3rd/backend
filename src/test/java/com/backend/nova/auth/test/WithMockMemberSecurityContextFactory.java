package com.backend.nova.auth.test;

import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.entity.Member;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithMockMemberSecurityContextFactory
        implements WithSecurityContextFactory<WithMockMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {

        // ✅ 가짜 Member 엔티티 생성
        Member member = Member.builder()
                .id(1L)
                .loginId("member")
                .name("테스트멤버")
                .loginType(LoginType.NORMAL)
                .build();

        MemberDetails memberDetails = new MemberDetails(member);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                memberDetails,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
