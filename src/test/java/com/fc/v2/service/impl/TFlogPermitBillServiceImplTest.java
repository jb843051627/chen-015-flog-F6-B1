package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.fc.v2.mapper.auto.TFlogPermitBillMapper;
import com.fc.v2.model.auto.TFlogPermitBill;

/**
 * 采伐许可证逐级核签单状态机测试：
 * 覆盖"否决后仍可签""入口口径不一""会签一票即走""退回票数不清"四类一线问题。
 */
public class TFlogPermitBillServiceImplTest {

    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    private TFlogPermitBillMapper mapper;
    private TFlogPermitBillServiceImpl service;

    @BeforeEach
    public void setUp() {
        mapper = Mockito.mock(TFlogPermitBillMapper.class);
        when(mapper.updateById(any())).thenReturn(1);
        service = new TFlogPermitBillServiceImpl();
        ReflectionTestUtils.setField(service, "flogPermitBillMapper", mapper);
    }

    private TFlogPermitBill bill(int node, Integer mode, int need, int signed, Integer status) {
        TFlogPermitBill r = new TFlogPermitBill();
        r.setId(1001L);
        r.setBillNo("HP-2026-001");
        r.setNodeNo(node);
        r.setSignMode(mode);
        r.setNeedCount(need);
        r.setSignCount(signed);
        r.setStatus(status);
        r.setDelFlag(0);
        when(mapper.selectById(1001L)).thenReturn(r);
        return r;
    }

    /** 场景1：点了不同意后单据留在原环节，但签批/否决/退回三个入口都不得再动 */
    @Test
    public void rejectClosesChainAndBlocksAllEntries() {
        TFlogPermitBill r = bill(1, MODE_AND, 2, 1, STATUS_RUNNING);

        TFlogPermitBill rejected = service.reject(1001L, "zhang", "不同意");
        assertEquals(STATUS_VETO, rejected.getStatus().intValue());
        assertEquals(1, rejected.getNodeNo().intValue(), "否决后单据停在原环节");
        assertEquals(0, rejected.getSignCount().intValue(), "否决时作废本轮票数");

        assertNull(service.approve(1001L, "li", "同意"), "已打回单据不得继续往上签");
        assertNull(service.reject(1001L, "wang", "再否决"), "已打回单据不得重复否决");
        assertNull(service.rollback(1001L, "退回"), "已打回单据不得退回");
        Mockito.verify(mapper, Mockito.times(1)).updateById(any());
    }

    /** 场景1b：已核讫的单据同样不得再操作（在途守卫三入口统一） */
    @Test
    public void passedBillCannotBeTouched() {
        bill(2, MODE_AND, 2, 0, STATUS_PASS);
        assertNull(service.approve(1001L, "zhang", "同意"));
        assertNull(service.reject(1001L, "zhang", "不同意"));
        assertNull(service.rollback(1001L, "退回"));
        verify(mapper, never()).updateById(any());
    }

    /** 场景2：会签需两人——只签一位不得往下走，签满第二位才推进 */
    @Test
    public void andModeWaitsForAllVotes() {
        TFlogPermitBill r = bill(0, MODE_AND, 2, 0, STATUS_RUNNING);

        TFlogPermitBill first = service.approve(1001L, "zhang", "同意");
        assertEquals(0, first.getNodeNo().intValue(), "会签第一位签完仍留在本环节");
        assertEquals(1, first.getSignCount().intValue());
        assertEquals(STATUS_RUNNING, first.getStatus().intValue());

        TFlogPermitBill second = service.approve(1001L, "li", "同意");
        assertEquals(1, second.getNodeNo().intValue(), "会签签满才进入下一环节");
        assertEquals(0, second.getSignCount().intValue(), "推进后清空票数");
    }

    /** 场景2b：或签一票即推进 */
    @Test
    public void orModeAdvancesOnSingleVote() {
        TFlogPermitBill r = bill(0, MODE_OR, 2, 0, STATUS_RUNNING);
        TFlogPermitBill done = service.approve(1001L, "zhang", "同意");
        assertEquals(1, done.getNodeNo().intValue());
        assertEquals(0, done.getSignCount().intValue());
    }

    /** 场景2c：末环节会签签满后结案为已核讫，不越界 */
    @Test
    public void lastNodeCompletesChain() {
        bill(2, MODE_AND, 2, 0, STATUS_RUNNING);
        service.approve(1001L, "zhang", "同意");
        TFlogPermitBill done = service.approve(1001L, "li", "同意");
        assertEquals(2, done.getNodeNo().intValue(), "环节号不越界");
        assertEquals(STATUS_PASS, done.getStatus().intValue());
        assertEquals(0, done.getSignCount().intValue());
    }

    /** 场景3：退回上一环节必须同步清零票数，残票不得带到退回后的环节 */
    @Test
    public void rollbackClearsVotes() {
        TFlogPermitBill r = bill(2, MODE_AND, 2, 2, STATUS_RUNNING);
        TFlogPermitBill back = service.rollback(1001L, "材料不齐退回重看");
        assertEquals(1, back.getNodeNo().intValue());
        assertEquals(0, back.getSignCount().intValue(), "退回后票数不得还挂在上面");
        assertEquals(STATUS_RUNNING, back.getStatus().intValue());
    }

    /** 场景3b：首环节退回不产生负环节，票数照样清零 */
    @Test
    public void rollbackAtFirstNodeStaysAtZero() {
        TFlogPermitBill r = bill(0, MODE_OR, 1, 1, STATUS_RUNNING);
        TFlogPermitBill back = service.rollback(1001L, "首环节退回");
        assertEquals(0, back.getNodeNo().intValue());
        assertEquals(0, back.getSignCount().intValue());
    }

    /** 场景4：入口口径统一——退回必须填写意见；签批/否决都必须有合法签署人 */
    @Test
    public void entriesShareSameGuards() {
        bill(1, MODE_OR, 1, 0, STATUS_RUNNING);
        assertNull(service.approve(1001L, null, "同意"), "无签署人不得签批");
        assertNull(service.approve(1001L, "  ", "同意"), "空白签署人不得签批");
        assertNull(service.reject(1001L, null, "不同意"), "无签署人不得否决（与签批同口径）");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 33; i++) {
            sb.append('x');
        }
        assertNull(service.approve(1001L, sb.toString(), "同意"), "超长签署人不得签批");
        assertNull(service.rollback(1001L, null), "无意见不得退回");
        assertNull(service.rollback(1001L, "  "), "空白意见不得退回");
        verify(mapper, never()).updateById(any());
    }

    /** 场景5：脏数据（环节越界/票数超过应签）三个入口一律拒绝 */
    @Test
    public void dirtyDataRejected() {
        bill(3, MODE_OR, 1, 0, STATUS_RUNNING);
        assertNull(service.approve(1001L, "zhang", "同意"));
        assertNull(service.reject(1001L, "zhang", "不同意"));
        assertNull(service.rollback(1001L, "退回"));

        bill(0, MODE_AND, 2, 3, STATUS_RUNNING);
        assertNull(service.approve(1001L, "zhang", "同意"));
        assertNull(service.reject(1001L, "zhang", "不同意"));
    }

    /** 存量数据缺省兜底：status/signMode/needCount/signCount 缺失时按或签 1 人正常流转 */
    @Test
    public void legacyNullFieldsDefaultToSingleVote() {
        TFlogPermitBill r = bill(0, null, 1, 0, null);
        r.setNeedCount(null);
        r.setSignCount(null);
        TFlogPermitBill done = service.approve(1001L, "zhang", "同意");
        assertEquals(1, done.getNodeNo().intValue());
        assertEquals(0, done.getSignCount().intValue());
    }

    /** 已删除单据不得操作 */
    @Test
    public void deletedBillRejected() {
        TFlogPermitBill r = bill(0, MODE_OR, 1, 0, STATUS_RUNNING);
        r.setDelFlag(1);
        assertNull(service.approve(1001L, "zhang", "同意"));
        assertNull(service.reject(1001L, "zhang", "不同意"));
        assertNull(service.rollback(1001L, "退回"));
        verify(mapper, never()).updateById(any());
    }
}
