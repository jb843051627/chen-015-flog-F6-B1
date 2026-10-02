package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 伐区采伐申请建档卡对象 t_flog_apply_card
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_flog_apply_card")
@ApiModel(value = "TFlogApplyCard", description = "伐区采伐申请建档卡")
public class TFlogApplyCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 伐区采伐申请建档卡号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "伐区采伐申请建档卡号")
    private String billNo;

    /** 本卡版本序 */
    @TableField("node_no")
    @ApiModelProperty(value = "本卡版本序")
    private Integer nodeNo;

    /** 所属编制单位在册底档 */
    @TableField("site_id")
    @ApiModelProperty(value = "所属编制单位在册底档")
    private Integer siteId;

    /** 所属单位底档字号 */
    @TableField("site_no")
    @ApiModelProperty(value = "所属单位底档字号")
    private String siteNo;

    /** 小班号（归属认底档字号配小班号这一对） */
    @TableField("subcomp_no")
    @ApiModelProperty(value = "小班号")
    private String subcompNo;

    /** 伐区名号（一卡管一小班之一片伐区） */
    @TableField("cut_block_no")
    @ApiModelProperty(value = "伐区名号")
    private String cutBlockNo;

    /** 采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造 */
    @TableField("cut_mode")
    @ApiModelProperty(value = "采伐方式")
    private Integer cutMode;

    /** 皆伐专属：株数 */
    @TableField("clear_trees")
    @ApiModelProperty(value = "皆伐专属株数")
    private Integer clearTrees;

    /** 版本现行标记 1现行 0旧版（列表只摆现行） */
    @TableField("current_flag")
    @ApiModelProperty(value = "版本现行标记")
    private Integer currentFlag;

    /** 本卡合并后拟采蓄积(立方米) */
    @TableField("qty")
    @ApiModelProperty(value = "本卡合并后拟采蓄积(立方米)")
    private BigDecimal qty;

    /** 本卡新报拟采面积(公顷) */
    @TableField("fine_amt")
    @ApiModelProperty(value = "本卡新报拟采面积(公顷)")
    private BigDecimal fineAmt;

    /** 核验串（随版本序走） */
    @TableField("content")
    @ApiModelProperty(value = "核验串（随版本序走）")
    private String content;

    /** 拟采强度折到的取用顺位 */
    @TableField("grade_level")
    @ApiModelProperty(value = "拟采强度折到的取用顺位")
    private Integer gradeLevel;

    /** 进展 0待核对 1已核对 2已压死 */
    @TableField("status")
    @ApiModelProperty(value = "进展 0待核对 1已核对 2已压死")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getSiteId() {
        return siteId;
    }

    public void setSiteId(Integer siteId) {
        this.siteId = siteId;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSubcompNo() {
        return subcompNo;
    }

    public void setSubcompNo(String subcompNo) {
        this.subcompNo = subcompNo;
    }

    public String getCutBlockNo() {
        return cutBlockNo;
    }

    public void setCutBlockNo(String cutBlockNo) {
        this.cutBlockNo = cutBlockNo;
    }

    public Integer getCutMode() {
        return cutMode;
    }

    public void setCutMode(Integer cutMode) {
        this.cutMode = cutMode;
    }

    public Integer getClearTrees() {
        return clearTrees;
    }

    public void setClearTrees(Integer clearTrees) {
        this.clearTrees = clearTrees;
    }

    public Integer getCurrentFlag() {
        return currentFlag;
    }

    public void setCurrentFlag(Integer currentFlag) {
        this.currentFlag = currentFlag;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getFineAmt() {
        return fineAmt;
    }

    public void setFineAmt(BigDecimal fineAmt) {
        this.fineAmt = fineAmt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
