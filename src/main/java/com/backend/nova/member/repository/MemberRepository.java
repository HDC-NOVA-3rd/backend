package com.backend.nova.member.repository;

import com.backend.nova.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByLoginId(String loginId);
    Optional<Member> findByLoginId(String loginId);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByResident_Id(Long residentId);
    Optional<Member> findFirstByResident_Ho_IdOrderByIdAsc(Long hoId);
    Optional<Member> findByNameAndPhoneNumber(String name, String phoneNumber);
    Optional<Member> findByLoginIdAndNameAndPhoneNumber(String loginId, String name, String phoneNumber);
    @Query("SELECT a.id FROM Member m " +
            "JOIN m.resident r " +
            "JOIN r.ho h " +
            "JOIN h.dong d " +
            "JOIN d.apartment a " +
            "WHERE m.id = :memberId")
    Optional<Long> findApartmentIdByMemberId(@Param("memberId") Long memberId);
}
