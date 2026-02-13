package com.backend.nova.auth.member;

import com.backend.nova.member.dto.MemberLocationResponse;
import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /* ================= loginId 기반 ================= */

    @Override
    public UserDetails loadUserByUsername(String loginId)
            throws UsernameNotFoundException {

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Member not found: " + loginId));

        MemberLocationResponse locationDto = memberRepository.findApartmentIdByMemberId(member.getId())
                .orElse(null);
        Long apartmentId = (locationDto != null) ? locationDto.apartmentId() : null;
        Long hoId = (locationDto != null) ? locationDto.hoId() : null;

        return new MemberDetails(member,apartmentId,hoId);
    }

    /* ================= ID 기반 (JWT 전용) ================= */
//
//    public UserDetails loadUserById(Long memberId) {
//        Member member = memberRepository.findById(memberId)
//                .orElseThrow(() ->
//                        new UsernameNotFoundException("Member not found id=" + memberId));
//        Long apartmentId = memberRepository.findApartmentIdByMemberId(member.getId())
//                .orElse(null);
//        return new MemberDetails(member,apartmentId);
//    }
}
