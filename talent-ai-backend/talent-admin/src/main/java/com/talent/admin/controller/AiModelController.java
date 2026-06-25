package com.talent.admin.controller;

import com.talent.admin.common.PageResult;
import com.talent.admin.common.Result;
import com.talent.admin.dto.AiModelQuery;
import com.talent.admin.dto.AiModelSaveRequest;
import com.talent.admin.dto.AiModelStatusRequest;
import com.talent.admin.dto.ModelStatsVO;
import com.talent.admin.dto.UsageDistVO;
import com.talent.admin.dto.UsageTrendVO;
import com.talent.admin.entity.AiModel;
import com.talent.admin.service.IAiModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 模型管理 控制器（admin 直连 talent_ai_db）
 *
 * @author TalentAI
 */
@RestController
@RequestMapping("/api/admin/ai-models")
@RequiredArgsConstructor
public class AiModelController {

    private final IAiModelService aiModelService;

    /** 列表 / 分页查询 */
    @GetMapping
    public Result<PageResult<AiModel>> page(AiModelQuery query) {
        return Result.success(aiModelService.pageQuery(query));
    }

    /** 新增模型 */
    @PostMapping
    public Result<Long> create(@RequestBody @Valid AiModelSaveRequest request) {
        return Result.success(aiModelService.createModel(request));
    }

    /** 编辑模型（含 promptTemplate） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid AiModelSaveRequest request) {
        aiModelService.updateModel(id, request);
        return Result.success();
    }

    /** 逻辑删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        aiModelService.deleteModel(id);
        return Result.success();
    }

    /** 启用 / 禁用 */
    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @RequestBody @Valid AiModelStatusRequest request) {
        aiModelService.changeStatus(id, request.getStatus());
        return Result.success();
    }

    /** 顶部统计卡 */
    @GetMapping("/stats")
    public Result<ModelStatsVO> stats() {
        return Result.success(aiModelService.stats());
    }

    /** 近7日调用趋势 */
    @GetMapping("/usage-trend")
    public Result<List<UsageTrendVO>> usageTrend() {
        return Result.success(aiModelService.usageTrend());
    }

    /** 各模型调用分布 */
    @GetMapping("/usage-distribution")
    public Result<List<UsageDistVO>> usageDistribution() {
        return Result.success(aiModelService.usageDistribution());
    }
}