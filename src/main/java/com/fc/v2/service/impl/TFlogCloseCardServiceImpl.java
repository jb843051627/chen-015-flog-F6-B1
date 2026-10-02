package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TFlogCloseCardMapper;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.service.ITFlogCloseCardService;

/**
 * 伐区勾销事务卡 Service业务层处理（state-machine 形状：单据流转）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogCloseCardServiceImpl implements ITFlogCloseCardService {

    private static final int MAX_STAGE = 3;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_TERMINAL = 2;

    @javax.annotation.Resource
    private TFlogCloseCardMapper flogCloseCardMapper;

    @Override
    public TFlogCloseCard selectTFlogCloseCardById(Long id) {
        return this.flogCloseCardMapper.selectById(id);
    }

    @Override
    public List<TFlogCloseCard> selectTFlogCloseCardList(QueryWrapper<TFlogCloseCard> queryWrapper) {
        return this.flogCloseCardMapper.selectList(queryWrapper);
    }

    @Override
    public TFlogCloseCard advance(Long id, String remark) {
        TFlogCloseCard r = this.flogCloseCardMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int st = r.getStage() == null ? 0 : r.getStage();
        r.setStage(Math.min(st + 2, MAX_STAGE));
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.flogCloseCardMapper.updateById(r);
        return r;
    }

    @Override
    public TFlogCloseCard rollback(Long id, String remark) {
        TFlogCloseCard r = this.flogCloseCardMapper.selectById(id);
        if (r == null) {
            return null;
        }
        r.setStage(0);
        r.setStatus(STATUS_ACTIVE);
        r.setLastAction(remark);
        this.flogCloseCardMapper.updateById(r);
        return r;
    }

    @Override
    public boolean updateContent(Long id, String remark) {
        TFlogCloseCard r = this.flogCloseCardMapper.selectById(id);
        if (r == null) {
            return false;
        }
        r.setContent(remark);
        return this.flogCloseCardMapper.updateById(r) > 0;
    }

    @Override
    public boolean remove(Long id) {
        TFlogCloseCard r = this.flogCloseCardMapper.selectById(id);
        if (r == null) {
            return false;
        }
        return this.flogCloseCardMapper.deleteById(id) > 0;
    }

}
