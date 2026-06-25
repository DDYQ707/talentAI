package com.talent.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.AiModelQuery;
import com.talent.admin.dto.AiModelSaveRequest;
import com.talent.admin.dto.ModelStatsVO;
import com.talent.admin.dto.UsageDistVO;
import com.talent.admin.dto.UsageTrendVO;
import com.talent.admin.entity.AiModel;

import java.util.List;

/**
 * AI 模型管理 服务
 *
 * @author TalentAI
 */
public interface IAiModelService extends IService<AiModel> {

    /** 条件分页查询 */
    PageResult<AiModel> pageQuery(AiModelQuery query);

    /** 新增模型 */
    Long createModel(AiModelSaveRequest request);

    /** 编辑模型（含 promptTemplate） */
    void updateModel(Long id, AiModelSaveRequest request);

    /** 逻辑删除 */
    void deleteModel(Long id);

    /** 启用 / 禁用 */
    void changeStatus(Long id, Integer status);

    /** 顶部统计卡 */
    ModelStatsVO stats();

    /** 近7日调用趋势（补齐零值） */
    List<UsageTrendVO> usageTrend();

    /** 各模型调用分布 */
    List<UsageDistVO> usageDistribution();
}