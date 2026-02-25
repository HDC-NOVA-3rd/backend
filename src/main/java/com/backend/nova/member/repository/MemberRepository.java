package com.backend.nova.member.repository;

import com.backend.nova.member.dto.MemberLocationResponse;
import com.backend.nova.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByLoginId(String loginId);
    Optional<Member> findByLoginId(String loginId);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByResident_Id(Long residentId);
    List<Member> findByResident_Ho_Dong_IdIn(Collection<Long> dongIds);
    Optional<Member> findFirstByResident_Ho_IdOrderByIdAsc(Long hoId);
    Optional<Member> findByNameAndPhoneNumber(String name, String phoneNumber);
    Optional<Member> findByLoginIdAndNameAndPhoneNumber(String loginId, String name, String phoneNumber);
    @Query("SELECT new com.backend.nova.member.dto.MemberLocationResponse(a.id, h.id) FROM Member m " +
            "JOIN m.resident r " +
            "JOIN r.ho h " +
            "JOIN h.dong d " +
            "JOIN d.apartment a " +
            "WHERE m.id = :memberId")
    Optional<MemberLocationResponse> findApartmentIdByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT m FROM Member m JOIN m.resident r JOIN r.ho h JOIN h.dong d WHERE d.apartment.id = :apartmentId AND m.pushToken IS NOT NULL AND m.pushToken <> ''")
    List<Member> findMembersWithPushTokenByApartmentId(@Param("apartmentId") Long apartmentId);
}
