package com.backend.nova.member.entity;

import org.springframework.security.test.context.support.WithSecurityContext;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockMemberSecurityContextFactory.class)
public @interface WithMockMember {
    long memberId() default 1L;
    String loginId() default "testUser";
    String password() default "password";
    String name() default "테스트유저";
    long apartmentId() default 100L;
    long hoId() default 100L;
}