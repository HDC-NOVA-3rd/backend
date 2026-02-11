package com.backend.nova.auth.member;

import com.backend.nova.member.entity.Member;
import com.backend.nova.member.repository.MemberRepository;
import com.backend.nova.resident.entity.Resident;
import com.backend.nova.resident.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /* ================= loginId 기반 ================= */

    @Override
    public UserDetails loadUserByUsername(String loginId)
            throws UsernameNotFoundException {

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Member not found: " + loginId));

        Resident resident = member.getResident();

        Long apartmentId = resident.getHo().getDong().getApartment().getId();
        Long hoId = resident.getHo().getId();

        return new MemberDetails(
                member,
                apartmentId,
                hoId,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
    }


    /* ================= ID 기반 (JWT 전용) ================= */

    public UserDetails loadUserById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Member not found id=" + memberId));

        Resident resident = member.getResident();

        return new MemberDetails(
                member,
                resident.getHo().getDong().getApartment().getId(),
                resident.getHo().getId(),
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))
        );
    }

}
