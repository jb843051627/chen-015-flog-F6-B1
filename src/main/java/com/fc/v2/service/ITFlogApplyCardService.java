package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TFlogApplyCard;

import java.util.List;

/**
 * 伐区采伐申请建档卡 Service接口
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITFlogApplyCardService {

    /** 按主键查询 */
    TFlogApplyCard selectTFlogApplyCardById(Long id);

    /** 按条件查询列表（分页由调用方统一处理） */
    List<TFlogApplyCard> selectTFlogApplyCardList(Wrapper<TFlogApplyCard> queryWrapper);

    /** 新增 */
    int insertTFlogApplyCard(TFlogApplyCard record);

    /** 修改 */
    int updateTFlogApplyCard(TFlogApplyCard record);

    /** 批量删除 */
    int deleteTFlogApplyCardByIds(String ids);

    /** 按主键删除 */
    int deleteTFlogApplyCardById(Long id);
}
