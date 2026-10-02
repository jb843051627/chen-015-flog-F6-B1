package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.service.ITFlogApplyCardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 伐区采伐申请建档卡 Controller
 *
 * @author fuce
 * @date 2026-09-12
 */
@Api(value = "伐区采伐申请建档卡")
@Controller
@RequestMapping("/FlogApplyCardController")
public class FlogApplyCardController extends BaseController {

    private final String prefix = "admin/flogApplyCard";

    @Autowired
    private ITFlogApplyCardService flogApplyCardService;

    @ApiOperation(value = "分页跳转", notes = "分页跳转")
    @GetMapping("/view")
    @RequiresPermissions("flog:flogApplyCard:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "伐区采伐申请建档卡集合查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("flog:flogApplyCard:list")
    @ResponseBody
    public ResultTable list(TFlogApplyCard record) {
        QueryWrapper<TFlogApplyCard> queryWrapper = new QueryWrapper<TFlogApplyCard>();
        startPage();
        com.github.pagehelper.PageInfo<TFlogApplyCard> page =
                new com.github.pagehelper.PageInfo<TFlogApplyCard>(flogApplyCardService.selectTFlogApplyCardList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "伐区采伐申请建档卡新增", action = "add")
    @ApiOperation(value = "新增", notes = "新增")
    @PostMapping("/add")
    @RequiresPermissions("flog:flogApplyCard:add")
    @ResponseBody
    public AjaxResult add(TFlogApplyCard record) {
        return toAjax(flogApplyCardService.insertTFlogApplyCard(record));
    }

    @Log(title = "伐区采伐申请建档卡修改", action = "edit")
    @ApiOperation(value = "修改保存", notes = "修改保存")
    @PostMapping("/edit")
    @RequiresPermissions("flog:flogApplyCard:edit")
    @ResponseBody
    public AjaxResult editSave(TFlogApplyCard record) {
        return toAjax(flogApplyCardService.updateTFlogApplyCard(record));
    }

    @Log(title = "伐区采伐申请建档卡删除", action = "remove")
    @ApiOperation(value = "删除", notes = "删除")
    @DeleteMapping("/remove")
    @RequiresPermissions("flog:flogApplyCard:remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(flogApplyCardService.deleteTFlogApplyCardByIds(ids));
    }
}
