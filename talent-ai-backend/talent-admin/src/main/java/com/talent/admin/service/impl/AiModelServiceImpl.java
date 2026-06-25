package com.talent.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.admin.common.PageResult;
import com.talent.admin.dto.AiModelQuery;
import com.talent.admin.dto.AiModelSaveRequest;
import com.talent.admin.dto.ModelStatsVO;
import com.talent.admin.dto.UsageDistVO;
import com.talent.admin.dto.UsageTrendVO;
import com.talent.admin.entity.AiModel;
import com.talent.admin.exception.BusinessException;
import com.talent.admin.mapper.AiModelMapper;
import com.talent.admin.service.IAiModelService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模型管理 服务实现
 *
 * @author TalentAI
 */
@Service
public class AiModelServiceImpl extends ServiceImpl<AiModelMapper, AiModel>
        implements IAiModelService {

    /** 估算单价：每 token 0.000002 元 */
    private static final BigDecimal UNIT_PRICE = new BigDecimal("0.000002");

    private static final DateTimeFormatter MD = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public PageResult<AiModel> pageQuery(AiModelQuery query) {
        int pageNum = (query.getPage() != null && query.getPage() > 0) ? query.getPage() : 1;
        int pageSize = (query.getSize() != null && query.getSize() > 0) ? query.getSize() : 10;

        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(AiModel::getModelName, kw)
                    .or().like(AiModel::getModelCode, kw));
        }
        if (query.getStatus() != null) {
            wrapper.eq(AiModel::getStatus, query.getStatus());
        }
        if (query.getModelType() != null) {
            wrapper.eq(AiModel::getModelType, query.getModelType());
        }
        wrapper.orderByDesc(AiModel::getCreatedAt);

        Page<AiModel> p = page(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(p);
    }

    @Override
    public Long createModel(AiModelSaveRequest request) {
        validateSave(request);
        // 编码唯一校验
        Long dup = lambdaQuery().eq(AiModel::getModelCode, request.getModelCode().trim()).count();
        if (dup != null && dup > 0) {
            throw new BusinessException("模型编码已存在：" + request.getModelCode());
        }
        AiModel model = new AiModel();
        model.setModelCode(request.getModelCode().trim());
        model.setModelName(request.getModelName().trim());
        model.setModelType(request.getModelType());
        model.setPurpose(request.getPurpose());
        model.setApiEndpoint(request.getApiEndpoint());
        model.setPromptTemplate(request.getPromptTemplate());
        model.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        if (!save(model)) {
            throw new BusinessException("新增模型失败");
        }
        return model.getId();
    }

    @Override
    public void updateModel(Long id, AiModelSaveRequest request) {
        validateSave(request);
        AiModel existing = getExisting(id);
        // 编码唯一校验（排除自身）
        Long dup = lambdaQuery()
                .eq(AiModel::getModelCode, request.getModelCode().trim())
                .ne(AiModel::getId, id)
                .count();
        if (dup != null && dup > 0) {
            throw new BusinessException("模型编码已存在：" + request.getModelCode());
        }
        existing.setModelCode(request.getModelCode().trim());
        existing.setModelName(request.getModelName().trim());
        existing.setModelType(request.getModelType());
        existing.setPurpose(request.getPurpose());
        existing.setApiEndpoint(request.getApiEndpoint());
        existing.setPromptTemplate(request.getPromptTemplate());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (!updateById(existing)) {
            throw new BusinessException("编辑模型失败");
        }
    }

    @Override
    public void deleteModel(Long id) {
        getExisting(id);
        if (!removeById(id)) {
            throw new BusinessException("删除模型失败");
        }
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(400, "状态值非法，仅支持 1(运行) 或 2(暂停)");
        }
        getExisting(id);
        AiModel update = new AiModel();
        update.setId(id);
        update.setStatus(status);
        if (!updateById(update)) {
            throw new BusinessException("状态更新失败");
        }
    }

    @Override
    public ModelStatsVO stats() {
        AiModelMapper mapper = getBaseMapper();
        ModelStatsVO vo = new ModelStatsVO();
        vo.setActiveModels(nz(mapper.countActiveModels()));
        vo.setTodayCalls(nz(mapper.countTodayCalls()));
        long totalTokens = nz(mapper.sumTokens());
        vo.setTotalTokens(totalTokens);
        vo.setEstimatedCost(UNIT_PRICE.multiply(BigDecimal.valueOf(totalTokens))
                .setScale(2, RoundingMode.HALF_UP));
        BigDecimal avg = mapper.avgLatencySeconds();
        vo.setAvgLatency(avg == null ? BigDecimal.ZERO : avg.setScale(2, RoundingMode.HALF_UP));
        return vo;
    }

    @Override
    public List<UsageTrendVO> usageTrend() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        // DB 查询（仅有数据的日期），key 为 MM-dd
        List<UsageTrendVO> raw = getBaseMapper().usageTrend(start.format(YMD));
        Map<String, Long> map = new LinkedHashMap<>();
        for (UsageTrendVO v : raw) {
            map.put(v.getDate(), v.getCalls());
        }
        // 补齐 7 天
        List<UsageTrendVO> result = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            String key = d.format(MD);
            result.add(new UsageTrendVO(key, map.getOrDefault(key, 0L)));
        }
        return result;
    }

    @Override
    public List<UsageDistVO> usageDistribution() {
        return getBaseMapper().usageDistribution();
    }

    /* ---------------- 私有工具 ---------------- */

    private void validateSave(AiModelSaveRequest request) {
        if (request == null) {
            throw new BusinessException(400, "请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getModelCode())) {
            throw new BusinessException(400, "模型编码不能为空");
        }
        if (!StringUtils.hasText(request.getModelName())) {
            throw new BusinessException(400, "模型名称不能为空");
        }
        if (request.getModelType() == null) {
            throw new BusinessException(400, "模型类型不能为空");
        }
        if (request.getStatus() != null && request.getStatus() != 1 && request.getStatus() != 2) {
            throw new BusinessException(400, "状态值非法");
        }
    }

    private AiModel getExisting(Long id) {
        if (id == null) {
            throw new BusinessException(400, "模型ID不能为空");
        }
        AiModel model = getById(id);
        if (model == null) {
            throw new BusinessException(404, "模型不存在或已删除");
        }
        return model;
    }

    private long nz(Long v) {
        return v == null ? 0L : v;
    }
}