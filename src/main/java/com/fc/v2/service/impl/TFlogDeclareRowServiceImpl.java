package com.fc.v2.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TFlogDeclareRowMapper;
import com.fc.v2.model.auto.TFlogDeclareRow;
import com.fc.v2.service.ITFlogDeclareRowService;

/**
 * 伐区申报清册核收行 Service业务层处理（batch-process 形状：整批提交）
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogDeclareRowServiceImpl implements ITFlogDeclareRowService {

    private static final int MAX_ROWS = 500;
    private static final int STATUS_OK = 1;
    private static final int STATUS_FAIL = 2;

    @javax.annotation.Resource
    private TFlogDeclareRowMapper flogDeclareRowMapper;

    @Override
    public TFlogDeclareRow selectTFlogDeclareRowById(Long id) {
        return this.flogDeclareRowMapper.selectById(id);
    }

    @Override
    public int submitBatch(String batchNo, List<TFlogDeclareRow> rows) {
        String no = rows.get(0).getBatchNo();
        java.util.List<TFlogDeclareRow> errors = new java.util.ArrayList<TFlogDeclareRow>();
        int seq = 0;
        for (TFlogDeclareRow r : rows) {
            if (r.getItemCode() == null || r.getItemCode().trim().isEmpty()
                    || r.getQty() == null
                    || r.getQty().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                seq++;
                r.setRowNo(Integer.valueOf(seq));
                r.setBatchNo(no);
                r.setStatus(STATUS_FAIL);
                this.flogDeclareRowMapper.insert(r);
                errors.add(r);
            }
        }
        if (!errors.isEmpty()) {
            return 0;
        }
        int ok = 0;
        for (TFlogDeclareRow r : rows) {
            r.setBatchNo(no);
            r.setStatus(STATUS_OK);
            this.flogDeclareRowMapper.insert(r);
            ok++;
        }
        return ok;
    }

    @Override
    public List<TFlogDeclareRow> listErrors(String batchNo) {
        return this.flogDeclareRowMapper.selectList(new QueryWrapper<TFlogDeclareRow>()
                .eq("batch_no", batchNo).eq("status", STATUS_FAIL));
    }
}
