package com.backend.nova.resident.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.backend.nova.resident.entity.Resident;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.backend.nova.resident.entity.QResident.resident;
import static com.backend.nova.apartment.entity.QHo.ho;
import static com.backend.nova.apartment.entity.QDong.dong;

@Repository
@RequiredArgsConstructor
public class ResidentQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Resident> findAllByFilters(Long apartmentId, Long dongId, String searchTerm, Pageable pageable) {
        List<Resident> content = queryFactory
                .selectFrom(resident)
                .join(resident.ho, ho).fetchJoin()
                .join(ho.dong, dong).fetchJoin()
                .where(
                        dong.apartment.id.eq(apartmentId),
                        dongIdEq(dongId),
                        searchTermContains(searchTerm)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .select(resident.count())
                .from(resident)
                .join(resident.ho, ho)
                .join(ho.dong, dong)
                .where(
                        dong.apartment.id.eq(apartmentId),
                        dongIdEq(dongId),
                        searchTermContains(searchTerm)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total);
    }

    private BooleanExpression dongIdEq(Long dongId) {
        return dongId != null ? dong.id.eq(dongId) : null;
    }

    private BooleanExpression searchTermContains(String searchTerm) {
        return StringUtils.hasText(searchTerm) ? resident.name.contains(searchTerm).or(resident.phone.contains(searchTerm)) : null;
    }
}