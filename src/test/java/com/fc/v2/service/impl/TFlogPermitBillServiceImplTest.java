package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fc.v2.mapper.auto.TFlogPermitBillMapper;
import com.fc.v2.model.auto.TFlogPermitBill;

/**
 * 采伐许可证核签单状态机单元测试：
 * 否决终态、三入口统一口径、会签满员才推进、退回清票。
 */
class TFlogPermitBillServiceImplTest {

    @Mock
    private TFlogPermitBillMapper mapper;

    @InjectMocks
    private TFlogPermitBillServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private TFlogPermitBill bill(Integer node, Integer mode, Integer need, Integer cnt, Integer status) {
        TFlogPermitBill r = new TFlogPermitBill();
        r.setId(1001L);
        r.setBillNo("PB-0001");
        r.setNodeNo(node);
        r.setSignMode(mode);
        r.setNeedCount(need);
        r.setSignCount(cnt);
        r.setStatus(status);
        r.setDelFlag(0);
        when(mapper.selectById(1001L)).thenReturn(r);
        return r;
    }

    // ---------- 或签 ----------

    @Test
    void orMode_oneVoteAdvancesAndClearsCount() {
        TFlogPermitBill r = bill(0, 0, 1, 0, 0);
        TFlogPermitBill out = service.approve(1001L, "zhang", "ok");
        assertSame(r, out);
        assertEquals(1, out.getNodeNo().intValue());
        assertEquals(0, out.getSignCount().intValue());
        assertEquals(0, out.getStatus().intValue());
        verify(mapper).updateById(r);
    }

    @Test
    void orMode_fourApprovalsAcrossNodesZeroToThree_thenPassed() {
        TFlogPermitBill r = bill(0, 0, 1, 0, 0);
        service.approve(1001L, "a", null); // -> 1
        service.approve(1001L, "a", null); // -> 2
        service.approve(1001L, "a", null); // -> 3
        TFlogPermitBill out = service.approve(1001L, "a", null); // 末环节满票 -> 已核讫
        assertEquals(3, out.getNodeNo().intValue(), "结案后环节号不得越出 0..3");
        assertEquals(1, out.getStatus().intValue());
        assertEquals(0, out.getSignCount().intValue());
        verify(mapper, times(4)).updateById(r);
    }

    @Test
    void nullModeAndNeedDefaultToOrSingleApprover() {
        TFlogPermitBill r = bill(0, null, null, null, 0);
        TFlogPermitBill out = service.approve(1001L, "a", null);
        assertEquals(1, out.getNodeNo().intValue());
        assertEquals(0, out.getSignCount().intValue());
    }

    @Test
    void legacyNullStatusTreatedAsInFlight() {
        TFlogPermitBill r = bill(0, 0, 1, 0, null);
        TFlogPermitBill out = service.approve(1001L, "a", null);
        assertEquals(1, out.getNodeNo().intValue());
    }

    // ---------- 会签 ----------

    @Test
    void andMode_partialVotesStayOnNode() {
        TFlogPermitBill r = bill(1, 1, 2, 0, 0);
        TFlogPermitBill first = service.approve(1001L, "a", null);
        assertEquals(1, first.getNodeNo().intValue(), "只签一位不得往下走");
        assertEquals(1, first.getSignCount().intValue());
        assertEquals(0, first.getStatus().intValue());
    }

    @Test
    void andMode_fullVotesAdvance() {
        TFlogPermitBill r = bill(1, 1, 2, 0, 0);
        service.approve(1001L, "a", null);
        TFlogPermitBill second = service.approve(1001L, "b", null);
        assertEquals(2, second.getNodeNo().intValue());
        assertEquals(0, second.getSignCount().intValue());
    }

    @Test
    void andMode_fullVotesOnLastNodePasses() {
        TFlogPermitBill r = bill(3, 1, 2, 0, 0);
        service.approve(1001L, "a", null);
        TFlogPermitBill second = service.approve(1001L, "b", null);
        assertEquals(3, second.getNodeNo().intValue());
        assertEquals(1, second.getStatus().intValue());
        assertEquals(0, second.getSignCount().intValue());
    }

    @Test
    void andMode_interruptedAdvanceIsCaughtUpWithoutDoubleCounting() {
        // 票数已满但上一轮推进中断：票数已是应签数，不得再加一票
        TFlogPermitBill r = bill(0, 1, 2, 2, 0);
        TFlogPermitBill out = service.approve(1001L, "a", null);
        assertEquals(1, out.getNodeNo().intValue());
        assertEquals(0, out.getSignCount().intValue());
        verify(mapper, times(1)).updateById(r);
    }

    // ---------- 否决 ----------

    @Test
    void reject_terminatesChainAsVeto() {
        TFlogPermitBill r = bill(1, 1, 2, 1, 0);
        TFlogPermitBill out = service.reject(1001L, "a", "不同意");
        assertEquals(2, out.getStatus().intValue(), "否决必须落成终态 已打回");
        assertEquals(0, out.getSignCount().intValue());
        assertEquals(1, out.getNodeNo().intValue());
    }

    @Test
    void afterVeto_allThreeEntriesRejectFurtherAction() {
        TFlogPermitBill r = bill(1, 1, 2, 1, 0);
        service.reject(1001L, "a", "不同意");

        assertNull(service.approve(1001L, "b", "再签也没用"));
        assertNull(service.reject(1001L, "b", null));
        assertNull(service.rollback(1001L, "退回也不行"));
        // 终态后不得再有任何落账
        verify(mapper, times(1)).updateById(r);
        assertEquals(2, r.getStatus().intValue());
    }

    @Test
    void afterPass_allEntriesRejectFurtherAction() {
        bill(3, 0, 1, 0, 1); // 已核讫
        assertNull(service.approve(1001L, "a", null));
        assertNull(service.reject(1001L, "a", null));
        assertNull(service.rollback(1001L, "x"));
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    // ---------- 退回 ----------

    @Test
    void rollback_movesNodeAndClearsCountTogether() {
        TFlogPermitBill r = bill(2, 1, 2, 2, 0);
        TFlogPermitBill out = service.rollback(1001L, "材料不全");
        assertEquals(1, out.getNodeNo().intValue());
        assertEquals(0, out.getSignCount().intValue(), "退回后票数不得还挂在原级");
        assertEquals(0, out.getStatus().intValue());
    }

    @Test
    void rollback_atFirstNodeStaysPutAndClearsCount() {
        TFlogPermitBill r = bill(0, 1, 2, 1, 0);
        TFlogPermitBill out = service.rollback(1001L, "重看");
        assertEquals(0, out.getNodeNo().intValue());
        assertEquals(0, out.getSignCount().intValue());
    }

    @Test
    void rollback_afterVetoRejected() {
        bill(2, 0, 1, 0, 2);
        assertNull(service.rollback(1001L, "x"));
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollback_blankCommentRejected() {
        TFlogPermitBill r = bill(1, 0, 1, 0, 0);
        assertNull(service.rollback(1001L, "   "));
        verify(mapper, never()).updateById(r);
    }

    // ---------- 入参与脏数据守卫 ----------

    @Test
    void approve_badApproverRejected() {
        TFlogPermitBill r = bill(0, 0, 1, 0, 0);
        assertNull(service.approve(1001L, null, null));
        assertNull(service.approve(1001L, "  ", null));
        assertNull(service.approve(1001L, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", null)); // 33 字
        verify(mapper, never()).updateById(r);
    }

    @Test
    void reject_blankApproverRejected() {
        TFlogPermitBill r = bill(0, 0, 1, 0, 0);
        assertNull(service.reject(1001L, "", null));
        verify(mapper, never()).updateById(r);
    }

    @Test
    void missingBillRejectedByAllEntries() {
        assertNull(service.approve(9999L, "a", null));
        assertNull(service.reject(9999L, "a", null));
        assertNull(service.rollback(9999L, "x"));
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletedBillRejectedByAllEntries() {
        bill(0, 0, 1, 0, 0).setDelFlag(1);
        assertNull(service.approve(1001L, "a", null));
        assertNull(service.reject(1001L, "a", null));
        assertNull(service.rollback(1001L, "x"));
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dirtyData_nodeOutOfRangeRejected() {
        bill(4, 0, 1, 0, 0); // 越出 0..3
        assertNull(service.approve(1001L, "a", null));
        assertNull(service.reject(1001L, "a", null));
        assertNull(service.rollback(1001L, "x"));
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dirtyData_badNeedOrCountRejected() {
        bill(0, 1, 0, 0, 0); // 应签人数非正
        assertNull(service.approve(1001L, "a", null));

        bill(0, 1, 2, 3, 0); // 票数超过应签数
        assertNull(service.approve(1001L, "a", null));

        bill(0, 1, 2, -1, 0); // 负票
        assertNull(service.approve(1001L, "a", null));
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void nullIdRejected() {
        assertNull(service.approve(null, "a", null));
        assertNull(service.reject(null, "a", null));
        assertNull(service.rollback(null, "x"));
        verify(mapper, never()).updateById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void selectByIdPassesThrough() {
        TFlogPermitBill r = bill(0, 0, 1, 0, 0);
        assertSame(r, service.selectTFlogPermitBillById(1001L));
    }
}
