//package com.backend.nova.auth.test;
//
//import com.backend.nova.auth.member.MemberDetails;
//import com.backend.nova.member.entity.LoginType;
//import com.backend.nova.member.entity.Member;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContext;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.test.context.support.WithSecurityContextFactory;
//
//import java.util.List;
//
//public class WithMockMemberSecurityContextFactory
//        implements WithSecurityContextFactory<WithMockMember> {
//
//    @Override
//    public SecurityContext createSecurityContext(WithMockMember annotation) {
//
//        Member member = Member.builder()
//                .id(annotation.memberId()) // ⭐ DB ID와 일치
//                .loginId("member")
//                .password("password")
//                .loginType(LoginType.NORMAL)
//                .build();
//
//        MemberDetails details = new MemberDetails(member);
//
//        Authentication auth = new UsernamePasswordAuthenticationToken(
//                details, null, details.getAuthorities()
//        );
//
//        SecurityContext context = SecurityContextHolder.createEmptyContext();
//        context.setAuthentication(auth);
//        return context;
//    }
//
//
//}
package com.backend.nova.auth.test;

import com.backend.nova.auth.member.MemberDetails;
import com.backend.nova.member.entity.LoginType;
import com.backend.nova.member.entity.Member;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockMemberSecurityContextFactory
        implements WithSecurityContextFactory<WithMockMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {

        Member member = Member.builder()
                .id(1L)
                .loginId("member")          // username
                .password("password")       // ⭐ 필수
                .name("테스트회원")           // ⭐ 권장
                .loginType(LoginType.NORMAL)
                .build();

        MemberDetails details = new MemberDetails(member);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
