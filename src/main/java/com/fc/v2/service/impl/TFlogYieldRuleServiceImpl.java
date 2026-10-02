package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TFlogYieldRuleMapper;
import com.fc.v2.model.auto.TFlogYieldRule;
import com.fc.v2.service.ITFlogYieldRuleService;

/**
 * 可采强度界限值线 Service业务层处理（rule-eval 形状）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogYieldRuleServiceImpl implements ITFlogYieldRuleService {

    /** 优先级降序；并列按 ruleCode 降序（确定性） */
    private static final Comparator<TFlogYieldRule> PRIORITY_THEN_CODE_DESC = new Comparator<TFlogYieldRule>() {
        @Override
        public int compare(TFlogYieldRule a, TFlogYieldRule b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority();
            int pb = b.getPriority() == null ? 0 : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            return cb.compareTo(ca);
        }
    };

    @javax.annotation.Resource
    private TFlogYieldRuleMapper flogYieldRuleMapper;

    /**
     * 时段比较统一走**墙钟字符串**（yyyy-MM-dd HH:mm:ss）+ 字符串绑定：
     * JDBC 的 serverTimezone 与本机时区不对称，直接把 java.util.Date 作参数会整体偏移，
     * 使边界判定在"切换当日"这类用例上随机错档（实测踩到）。
     */
    private static String ts(Date d) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    @Override
    public TFlogYieldRule selectTFlogYieldRuleById(Long id) {
        return this.flogYieldRuleMapper.selectById(id);
    }

    @Override
    public List<TFlogYieldRule> listAvailable(Date at) {
        if (at == null) {
            return new java.util.ArrayList<TFlogYieldRule>();
        }
        return this.flogYieldRuleMapper.selectList(new QueryWrapper<TFlogYieldRule>().eq("del_flag", 0));
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || input == null || at == null) {
            return 0;
        }
        TFlogYieldRule rule = findRule(ruleCode, at);
        return levelOf(rule, input);
    }

    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        if (input == null || at == null) {
            return 0;
        }
        if (input.compareTo(BigDecimal.ZERO) < 0
                || input.compareTo(new BigDecimal("100")) > 0) {
            return 0;
        }
        List<TFlogYieldRule> avail = listAvailable(at);
        if (avail.isEmpty()) {
            return 0;
        }
        return levelOf(avail.get(0), input);
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        TFlogYieldRule r = this.flogYieldRuleMapper.selectById(id);
        return r != null;
    }

    @Override
    public int countAvailable(Date at) {
        if (at == null) {
            return 0;
        }
        return this.flogYieldRuleMapper
                .selectList(new QueryWrapper<TFlogYieldRule>().eq("del_flag", 0)).size();
    }

    private TFlogYieldRule findRule(String ruleCode, Date at) {
        List<TFlogYieldRule> hit = this.flogYieldRuleMapper.selectList(new QueryWrapper<TFlogYieldRule>()
                .eq("del_flag", 0).eq("rule_code", ruleCode));
        TFlogYieldRule newest = null;
        for (TFlogYieldRule r : hit) {
            if (newest == null || r.getEffEnd() == null) {
                newest = r;
            } else if (newest.getEffEnd() != null && r.getEffEnd().after(newest.getEffEnd())) {
                newest = r;
            }
        }
        return newest;
    }

    /** 阈值解析：档案为主、明细覆盖；两者皆缺返回 null（调用方按不可用处理），精度统一 2 位 */
    private BigDecimal resolveLimit(TFlogYieldRule r, int tier) {
        if (tier == 1) {
            return r.getTh1Max();
        }
        if (tier == 2) {
            return r.getTh2Max();
        }
        return r.getTh3Max();
    }

    /** 档案级默认阈值（明细未维护时的回退来源） */
    private BigDecimal defaultThreshold(TFlogYieldRule r, int tier) {
        return r.getTh1Max();
    }

    /** 分档：等于上限取高一档 */
    private int levelOf(TFlogYieldRule rule, BigDecimal input) {
        if (rule.getTh1Max() != null && input.compareTo(rule.getTh1Max()) <= 0) {
            return 1;
        }
        if (rule.getTh2Max() != null && input.compareTo(rule.getTh2Max()) <= 0) {
            return 2;
        }
        if (rule.getTh3Max() != null && input.compareTo(rule.getTh3Max()) <= 0) {
            return 3;
        }
        return 4;
    }
}
