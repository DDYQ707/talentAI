package com.talent.pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.common.api.R;
import com.talent.pool.dto.TagBindRequest;
import com.talent.pool.dto.TalentPoolArchiveRequest;
import com.talent.pool.dto.TalentPoolQueryRequest;
import com.talent.pool.dto.TalentPoolUpdateRequest;
import com.talent.pool.entity.TalentPoolRecord;
import com.talent.pool.entity.TalentPoolTag;
import com.talent.pool.entity.TalentTag;
import com.talent.pool.feign.AuthFeignClient;
import com.talent.pool.feign.JobFeignClient;
import com.talent.pool.feign.ResumeFeignClient;
import com.talent.pool.mapper.TalentPoolRecordMapper;
import com.talent.pool.service.ITalentPoolRecordService;
import com.talent.pool.service.ITalentPoolTagService;
import com.talent.pool.service.ITalentTagService;
import com.talent.pool.util.FeignResponseHelper;
import com.talent.pool.vo.TalentPoolRecordVO;
import com.talent.pool.vo.TalentTagVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 人才库记录表 服务实现类
 *
 * @author TalentAI
 * @since 2026-06-17
 */
@Service
@Slf4j
public class TalentPoolRecordServiceImpl extends ServiceImpl<TalentPoolRecordMapper, TalentPoolRecord> implements ITalentPoolRecordService {

    private static final byte DEFAULT_JOB_SEEKING_STATUS = 3;

    @Autowired
    private ITalentPoolTagService talentPoolTagService;

    @Autowired
    private ITalentTagService talentTagService;

    @Autowired
    private JobFeignClient jobFeignClient;

    @Autowired
    private AuthFeignClient authFeignClient;

    @Autowired
    private ResumeFeignClient resumeFeignClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<TalentPoolRecordVO> archive(TalentPoolArchiveRequest req, Long operatorId) {
        if (req == null || req.getCandidateId() == null) {
            return R.fail("候选人ID不能为空");
        }
        if (!StringUtils.hasText(req.getCandidateName())) {
            return R.fail("候选人姓名不能为空");
        }

        // 检查候选人是否已在人才库中
        long existCount = count(new LambdaQueryWrapper<TalentPoolRecord>()
                .eq(TalentPoolRecord::getCandidateId, req.getCandidateId()));
        if (existCount > 0) {
            return R.fail("该候选人已在人才库中，请勿重复归档");
        }

        enrichArchiveRequest(req);

        LocalDateTime now = LocalDateTime.now();
        TalentPoolRecord record = new TalentPoolRecord();
        record.setCandidateId(req.getCandidateId());
        record.setCandidateName(req.getCandidateName().trim());
        record.setCurrentTitle(req.getCurrentTitle());
        record.setResumeId(req.getResumeId());
        record.setSourceApplicationId(req.getSourceApplicationId());
        record.setSourceJobTitle(req.getSourceJobTitle());
        record.setInterviewSummary(req.getInterviewSummary());
        record.setTalentCategory(req.getTalentCategory());
        record.setJobSeekingStatus(
                req.getJobSeekingStatus() != null ? req.getJobSeekingStatus() : DEFAULT_JOB_SEEKING_STATUS);
        record.setMatchScore(req.getMatchScore());
        record.setCurrentCompany(req.getCurrentCompany());
        record.setIsSaved(false);
        record.setArchiveReason(req.getArchiveReason());
        record.setArchivedBy(operatorId);
        record.setArchivedAt(now);

        if (!save(record)) {
            return R.fail("归档保存失败");
        }

        TalentPoolRecordVO vo = toRecordVO(record);
        vo.setTags(new ArrayList<>());
        return R.ok(vo);
    }

    @Override
    public R<Void> deleteRecord(Long id) {
        if (id == null) {
            return R.fail("记录ID不能为空");
        }
        TalentPoolRecord record = getById(id);
        if (record == null) {
            return R.fail("记录不存在");
        }
        if (!removeById(id)) {
            return R.fail("删除失败");
        }
        return R.ok();
    }

    @Override
    public R<Void> updateRecord(Long id, TalentPoolUpdateRequest req) {
        if (id == null) {
            return R.fail("记录ID不能为空");
        }
        if (req == null) {
            return R.fail("请求参数不能为空");
        }
        TalentPoolRecord record = getById(id);
        if (record == null) {
            return R.fail("记录不存在");
        }

        if (req.getCurrentTitle() != null) {
            record.setCurrentTitle(req.getCurrentTitle());
        }
        if (req.getTalentCategory() != null) {
            record.setTalentCategory(req.getTalentCategory());
        }
        if (req.getJobSeekingStatus() != null) {
            record.setJobSeekingStatus(req.getJobSeekingStatus());
        }
        if (req.getMatchScore() != null) {
            record.setMatchScore(req.getMatchScore());
        }
        if (req.getCurrentCompany() != null) {
            record.setCurrentCompany(req.getCurrentCompany());
        }
        if (req.getIsSaved() != null) {
            record.setIsSaved(req.getIsSaved());
        }
        if (req.getArchiveReason() != null) {
            record.setArchiveReason(req.getArchiveReason());
        }

        if (!updateById(record)) {
            return R.fail("更新失败");
        }
        return R.ok();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> bindTags(Long poolRecordId, TagBindRequest req) {
        if (poolRecordId == null) {
            return R.fail("人才库记录ID不能为空");
        }
        if (req == null || req.getTagIds() == null || req.getTagIds().isEmpty()) {
            return R.fail("标签ID列表不能为空");
        }

        // 校验记录存在
        TalentPoolRecord record = getById(poolRecordId);
        if (record == null) {
            return R.fail("人才库记录不存在");
        }

        // 校验标签存在
        List<TalentTag> tags = talentTagService.listByIds(req.getTagIds());
        if (tags.isEmpty()) {
            return R.fail("指定的标签不存在");
        }
        Set<Long> validTagIds = tags.stream().map(TalentTag::getId).collect(Collectors.toSet());

        // 查出已绑定的标签ID，避免重复
        List<TalentPoolTag> existBinds = talentPoolTagService.list(
                new LambdaQueryWrapper<TalentPoolTag>()
                        .eq(TalentPoolTag::getPoolRecordId, poolRecordId)
                        .in(TalentPoolTag::getTagId, validTagIds));
        Set<Long> existTagIds = existBinds.stream().map(TalentPoolTag::getTagId).collect(Collectors.toSet());

        // 过滤出需要新增的
        List<TalentPoolTag> newBinds = validTagIds.stream()
                .filter(tagId -> !existTagIds.contains(tagId))
                .map(tagId -> {
                    TalentPoolTag pt = new TalentPoolTag();
                    pt.setPoolRecordId(poolRecordId);
                    pt.setTagId(tagId);
                    return pt;
                })
                .collect(Collectors.toList());

        if (!newBinds.isEmpty()) {
            if (!talentPoolTagService.saveBatch(newBinds)) {
                return R.fail("标签绑定失败");
            }
        }
        return R.ok();
    }

    @Override
    public R<Void> unbindTag(Long poolRecordId, Long tagId) {
        if (poolRecordId == null || tagId == null) {
            return R.fail("参数不能为空");
        }
        boolean removed = talentPoolTagService.remove(
                new LambdaQueryWrapper<TalentPoolTag>()
                        .eq(TalentPoolTag::getPoolRecordId, poolRecordId)
                        .eq(TalentPoolTag::getTagId, tagId));
        if (!removed) {
            return R.fail("标签关联不存在或已移除");
        }
        return R.ok();
    }

    @Override
    public R<Map<String, Object>> listPage(TalentPoolQueryRequest req) {
        int pageNum = (req.getCurrent() != null && req.getCurrent() > 0) ? req.getCurrent() : 1;
        int pageSize = (req.getSize() != null && req.getSize() > 0) ? Math.min(req.getSize(), 50) : 20;

        Page<TalentPoolRecordVO> page = new Page<>(pageNum, pageSize);
        List<TalentPoolRecordVO> records = baseMapper.selectPoolRecordPage(
                page,
                req.getJobSeekingStatus(),
                req.getMinScore(),
                req.getMaxScore(),
                req.getTagId(),
                StringUtils.hasText(req.getKeyword()) ? req.getKeyword().trim() : null
        );

        Map<String, Object> data = new HashMap<>();
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("pages", page.getPages());
        data.put("records", records);
        return R.ok(data);
    }

    @Override
    public boolean existsByCandidateId(Long candidateId) {
        if (candidateId == null) {
            return false;
        }
        return count(new LambdaQueryWrapper<TalentPoolRecord>().eq(TalentPoolRecord::getCandidateId, candidateId)) > 0;
    }

    @Override
    public Map<Long, Boolean> existsByCandidateIds(List<Long> candidateIds) {
        Map<Long, Boolean> result = new LinkedHashMap<>();
        if (candidateIds == null || candidateIds.isEmpty()) {
            return result;
        }
        for (Long candidateId : candidateIds) {
            if (candidateId == null) {
                continue;
            }
            result.put(candidateId, existsByCandidateId(candidateId));
        }
        return result;
    }

    private void enrichArchiveRequest(TalentPoolArchiveRequest req) {
        if (req.getSourceApplicationId() != null) {
            try {
                Map<String, Object> res = jobFeignClient.applicationById(req.getSourceApplicationId());
                if (FeignResponseHelper.code(res) == 200) {
                    Map<String, Object> data = FeignResponseHelper.dataMap(res);
                    if (req.getMatchScore() == null) {
                        Integer score = FeignResponseHelper.intVal(data.get("matchScore"));
                        if (score != null) {
                            req.setMatchScore((byte) Math.max(0, Math.min(100, score)));
                        }
                    }
                    if (!StringUtils.hasText(req.getSourceJobTitle())) {
                        req.setSourceJobTitle(FeignResponseHelper.strVal(data.get("jobTitle")));
                    }
                    if (req.getResumeId() == null) {
                        req.setResumeId(FeignResponseHelper.longVal(data.get("resumeId")));
                    }
                    if (!StringUtils.hasText(req.getCandidateName())) {
                        req.setCandidateName(FeignResponseHelper.strVal(data.get("candidateName")));
                    }
                }
            } catch (Exception e) {
                log.debug("归档补全投递信息失败 applicationId={} {}", req.getSourceApplicationId(), e.getMessage());
            }
        }

        try {
            Map<String, Object> brief = authFeignClient.candidateBrief(req.getCandidateId());
            if (brief != null && !brief.isEmpty()) {
                if (!StringUtils.hasText(req.getCandidateName())) {
                    req.setCandidateName(FeignResponseHelper.strVal(brief.get("realName")));
                }
                if (!StringUtils.hasText(req.getCurrentTitle())) {
                    req.setCurrentTitle(FeignResponseHelper.strVal(brief.get("currentTitle")));
                }
                if (req.getJobSeekingStatus() == null) {
                    Integer status = FeignResponseHelper.intVal(brief.get("jobSeekingStatus"));
                    if (status != null) {
                        req.setJobSeekingStatus((byte) status.intValue());
                    }
                }
                if (req.getMatchScore() == null) {
                    Integer aiScore = FeignResponseHelper.intVal(brief.get("aiScore"));
                    if (aiScore != null) {
                        req.setMatchScore((byte) Math.max(0, Math.min(100, aiScore)));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("归档补全候选人档案失败 candidateId={} {}", req.getCandidateId(), e.getMessage());
        }

        if (req.getResumeId() != null) {
            try {
                Map<String, Object> res = resumeFeignClient.hrResumeBrief(req.getResumeId());
                if (FeignResponseHelper.code(res) == 200) {
                    Map<String, Object> data = FeignResponseHelper.dataMap(res);
                    if (!StringUtils.hasText(req.getCurrentTitle())) {
                        req.setCurrentTitle(FeignResponseHelper.strVal(data.get("currentTitle")));
                    }
                    if (req.getMatchScore() == null) {
                        Integer score = FeignResponseHelper.intVal(data.get("matchScore"));
                        if (score != null) {
                            req.setMatchScore((byte) Math.max(0, Math.min(100, score)));
                        }
                    }
                    if (!StringUtils.hasText(req.getSourceJobTitle())) {
                        req.setSourceJobTitle(FeignResponseHelper.strVal(data.get("appliedJobTitle")));
                    }
                    if (!StringUtils.hasText(req.getCurrentCompany())) {
                        req.setCurrentCompany(extractLatestCompany(data.get("workExperiences")));
                    }
                }
            } catch (Exception e) {
                log.debug("归档补全简历信息失败 resumeId={} {}", req.getResumeId(), e.getMessage());
            }
        }

        if (req.getJobSeekingStatus() == null) {
            req.setJobSeekingStatus(DEFAULT_JOB_SEEKING_STATUS);
        }
    }

    private String extractLatestCompany(Object workExperiences) {
        if (!(workExperiences instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (first instanceof Map<?, ?> work) {
            return FeignResponseHelper.strVal(work.get("companyName"));
        }
        return null;
    }

    /**
     * Entity → VO 转换
     */
    private TalentPoolRecordVO toRecordVO(TalentPoolRecord record) {
        TalentPoolRecordVO vo = new TalentPoolRecordVO();
        vo.setId(record.getId());
        vo.setCandidateId(record.getCandidateId());
        vo.setCandidateName(record.getCandidateName());
        vo.setCurrentTitle(record.getCurrentTitle());
        vo.setResumeId(record.getResumeId());
        vo.setSourceApplicationId(record.getSourceApplicationId());
        vo.setSourceJobTitle(record.getSourceJobTitle());
        vo.setInterviewSummary(record.getInterviewSummary());
        vo.setTalentCategory(record.getTalentCategory());
        vo.setJobSeekingStatus(record.getJobSeekingStatus());
        vo.setMatchScore(record.getMatchScore());
        vo.setCurrentCompany(record.getCurrentCompany());
        vo.setIsSaved(record.getIsSaved());
        vo.setArchiveReason(record.getArchiveReason());
        vo.setArchivedBy(record.getArchivedBy());
        vo.setArchivedAt(record.getArchivedAt());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
