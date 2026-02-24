package com.backend.nova.bill.repository;

import com.backend.nova.bill.dto.BillSearchCondition;
import com.backend.nova.bill.dto.BillSummaryResponse;
import com.backend.nova.bill.entity.BillStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.backend.nova.apartment.entity.QApartment.apartment;
import static com.backend.nova.bill.entity.QBill.bill;
import static com.backend.nova.apartment.entity.QHo.ho;
import static com.backend.nova.apartment.entity.QDong.dong;
import static org.springframework.util.StringUtils.hasText;


@Repository
@RequiredArgsConstructor
public class BillQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 관리자용 페이징 조회 (미납 필터 추가)
    public Page<BillSummaryResponse> findAllByAdmin(Long apartmentId, BillSearchCondition condition, Pageable pageable) {
        List<BillSummaryResponse> content = queryFactory
                .select(Projections.constructor(BillSummaryResponse.class,
                        bill.id, apartment.name, dong.dongNo, ho.hoNo,
                        bill.billMonth, bill.totalPrice, bill.status, bill.dueDate))
                .from(bill)
                .join(bill.ho, ho)
                .join(ho.dong, dong)
                .join(dong.apartment, apartment)
                .where(
                        apartment.id.eq(apartmentId),
                        dongNoEq(condition.getDongNo()),
                        hoNoEq(condition.getHoNo()),
                        billMonthEq(condition.getBillMonth()),
                        unpaidOnly(condition.getOnlyUnpaid()) // 미납 필터 추가
                )
                .orderBy(bill.billMonth.desc(), dong.dongNo.asc(), ho.hoNo.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(bill.count())
                .from(bill)
                .join(bill.ho, ho)
                .where(
                        ho.dong.apartment.id.eq(apartmentId),
                        unpaidOnly(condition.getOnlyUnpaid())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // 엑셀 다운로드용 전체 조회 (페이징 없음)
    public List<BillSummaryResponse> findAllForExcel(Long apartmentId, BillSearchCondition condition) {
        return queryFactory
                .select(Projections.constructor(BillSummaryResponse.class,
                        bill.id, apartment.name, dong.dongNo, ho.hoNo,
                        bill.billMonth, bill.totalPrice, bill.status, bill.dueDate))
                .from(bill)
                .join(bill.ho, ho)
                .join(ho.dong, dong)
                .join(dong.apartment, apartment)
                .where(
                        apartment.id.eq(apartmentId),
                        dongNoEq(condition.getDongNo()),
                        hoNoEq(condition.getHoNo()),
                        billMonthEq(condition.getBillMonth()),
                        unpaidOnly(condition.getOnlyUnpaid())
                )
                .orderBy(dong.dongNo.asc(), ho.hoNo.asc()) // 엑셀은 보통 동호수 순 정렬
                .fetch();
    }

    // 사용자용 페이징 조회
    public Page<BillSummaryResponse> findAllByMember(Long hoId, Pageable pageable) {
        List<BillSummaryResponse> content = queryFactory
                .select(Projections.constructor(BillSummaryResponse.class,
                        bill.id,
                        apartment.name,
                        dong.dongNo,
                        ho.hoNo,
                        bill.billMonth,
                        bill.totalPrice,
                        bill.status,
                        bill.dueDate
                ))
                .from(bill)
                .join(bill.ho, ho)
                .join(ho.dong, dong)
                .join(dong.apartment, apartment)
                .where(ho.id.eq(hoId))
                .orderBy(bill.billMonth.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(bill.count())
                .from(bill)
                .where(bill.ho.id.eq(hoId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }



    // 동적 조건 메서드들

    private BooleanExpression unpaidOnly(Boolean onlyUnpaid) {
        // null이거나 false면 조건 없음, true면 OVERDUE 상태만 조회
        return (onlyUnpaid != null && onlyUnpaid) ? bill.status.eq(BillStatus.OVERDUE) : null;
    }

    private BooleanExpression dongNoEq(String dongNo) {
        return hasText(dongNo) ? dong.dongNo.eq(dongNo) : null;
    }

    private BooleanExpression hoNoEq(String hoNo) {
        return hasText(hoNo) ? ho.hoNo.eq(hoNo) : null;
    }

    private BooleanExpression billMonthEq(String billMonth) {
        return hasText(billMonth) ? bill.billMonth.eq(billMonth) : null;
    }

    private BooleanExpression statusEq(BillStatus status) {
        return status != null ? bill.status.eq(status) : null;
    }
}
