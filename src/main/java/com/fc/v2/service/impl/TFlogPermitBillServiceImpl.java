package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;

import com.fc.v2.mapper.auto.TFlogPermitBillMapper;
import com.fc.v2.model.auto.TFlogPermitBill;
import com.fc.v2.service.ITFlogPermitBillService;

/**
 * 采伐许可证逐级核签单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * 口径约定（签批 / 否决 / 退回三个入口同一出处，不得另判）：
 * 1.仅在核（status=0；存量缺失按在核兜底）单据可操作，已核讫/已打回一律拒绝；
 * 2.或签一票即推进，会签须签满本级应签人数才推进，未签足只计票不挪环节；
 * 3.否决立即置"已打回"终态并作废本轮票数，单据停在原环节但不得再签；
 * 4.退回上一环节同时清零票数，杜绝上一轮残票带到退回后的环节。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogPermitBillServiceImpl implements ITFlogPermitBillService {

    private static final int MAX_NODE = 2;
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    @javax.annotation.Resource
    private TFlogPermitBillMapper flogPermitBillMapper;

    @Override
    public TFlogPermitBill selectTFlogPermitBillById(Long id) {
        return this.flogPermitBillMapper.selectById(id);
    }

    @Override
    public TFlogPermitBill approve(Long id, String approver, String comment) {
        TFlogPermitBill r = loadForAction(id, approver);
        if (r == null) {
            return null;
        }
        int mode = resolveMode(r);
        int need = resolveNeed(r);
        int cnt = vote(r);
        if (cnt >= need) {
            // 本级票已签满（如会签满员后上一轮推进被中断的存量数据）：只补推进，绝不重复计票
            advance(r);
            this.flogPermitBillMapper.updateById(r);
            return r;
        }
        cnt = cnt + 1;
        r.setSignCount(Integer.valueOf(cnt));
        // 或签一票即满、会签满员才推进；未签足时留在原环节继续等票（推进/结案口径唯一出处）
        if (mode == MODE_OR || cnt >= need) {
            advance(r);
        }
        this.flogPermitBillMapper.updateById(r);
        return r;
    }

    @Override
    public TFlogPermitBill reject(Long id, String approver, String comment) {
        TFlogPermitBill r = loadForAction(id, approver);
        if (r == null) {
            return null;
        }
        // 否决即终止整条链：置"已打回"终态并作废本轮签署，单据留在原环节且此后三入口均不得再动
        r.setStatus(Integer.valueOf(STATUS_VETO));
        r.setSignCount(Integer.valueOf(0));
        this.flogPermitBillMapper.updateById(r);
        return r;
    }

    @Override
    public TFlogPermitBill rollback(Long id, String comment) {
        if (id == null) {
            return null;
        }
        if (comment == null || comment.trim().isEmpty()) {
            return null;
        }
        TFlogPermitBill r = findRunning(id);
        if (r == null) {
            return null;
        }
        // 结构合法性校验：环节越界/应签人数非正/票数越界 一律视为脏数据，不予退回
        if (!sane(r)) {
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        int prev = node - 1;
        if (prev < 0) {
            // 已在首环节：退回原地，绝不产生负环节（脏数据会把链彻底走死）
            prev = 0;
        }
        // 退回即作废本轮签署：环节回退后票数必须同步清零，重看后按新环节重新计票
        r.setNodeNo(Integer.valueOf(prev));
        r.setSignCount(Integer.valueOf(0));
        this.flogPermitBillMapper.updateById(r);
        return r;
    }

    /** 签批/否决统一前置：身份校验 + 在途守卫 + 结构校验，两个入口同一出处 */
    private TFlogPermitBill loadForAction(Long id, String approver) {
        if (id == null) {
            return null;
        }
        if (approver == null || approver.trim().isEmpty()) {
            return null;
        }
        if (approver.trim().length() > 32) {
            return null;
        }
        TFlogPermitBill r = findRunning(id);
        if (r == null) {
            return null;
        }
        if (!sane(r)) {
            return null;
        }
        return r;
    }

    /**
     * 统一在途守卫（三入口同一出处）：不存在、已删除、已结案（已核讫/已打回）一律返回 null。
     * status 缺失的存量数据按"在核"放行，交由 sane() 再做结构校验。
     */
    private TFlogPermitBill findRunning(Long id) {
        if (id == null) {
            return null;
        }
        TFlogPermitBill r = this.flogPermitBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        if (r.getDelFlag() != null && r.getDelFlag().intValue() != 0) {
            return null;
        }
        if (r.getStatus() != null && r.getStatus().intValue() != STATUS_RUNNING) {
            return null;
        }
        return r;
    }

    /** 结构校验：环节越界、应签人数非正、票数为负或超过应签数，均视为脏数据不予签批 */
    private boolean sane(TFlogPermitBill r) {
        if (r == null) {
            return false;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        if (node < 0 || node > MAX_NODE) {
            return false;
        }
        int need = r.getNeedCount() == null ? 1 : r.getNeedCount();
        if (need <= 0) {
            return false;
        }
        int cnt = r.getSignCount() == null ? 0 : r.getSignCount();
        if (cnt < 0 || cnt > need) {
            return false;
        }
        return true;
    }

    /** 签批模式解析：缺省按或签（单人签批）处理，未知取值也归或签，避免链卡死 */
    private int resolveMode(TFlogPermitBill r) {
        if (r.getSignMode() == null) {
            return MODE_OR;
        }
        int m = r.getSignMode().intValue();
        return (m == MODE_AND) ? MODE_AND : MODE_OR;
    }

    /** 应签人数解析：缺省按 1 人；非正值按 1 人兜底，杜绝"签 0 票即过" */
    private int resolveNeed(TFlogPermitBill r) {
        if (r.getNeedCount() == null) {
            return 1;
        }
        int n = r.getNeedCount().intValue();
        return n > 0 ? n : 1;
    }

    /** 当前票数：缺省 0；负值按 0 计（脏数据不放大票数） */
    private int vote(TFlogPermitBill r) {
        if (r.getSignCount() == null) {
            return 0;
        }
        int c = r.getSignCount().intValue();
        return c > 0 ? c : 0;
    }

    /** 推进环节：未到末环节则进入下一环节并清空本轮票数；末环节签满后置为已核讫且不再越界 */
    private void advance(TFlogPermitBill r) {
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        int next = node + 1;
        if (next > MAX_NODE) {
            r.setStatus(Integer.valueOf(STATUS_PASS));
            r.setSignCount(Integer.valueOf(0));
            return;
        }
        r.setNodeNo(Integer.valueOf(next));
        r.setSignCount(Integer.valueOf(0));
    }

}
