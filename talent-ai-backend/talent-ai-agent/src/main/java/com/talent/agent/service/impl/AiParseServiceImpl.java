package com.talent.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.agent.domain.dto.ParseTaskRequest;
import com.talent.agent.domain.entity.AiParseTask;
import com.talent.agent.domain.vo.ParseTaskVO;
import com.talent.agent.mapper.AiParseTaskMapper;
import com.talent.agent.service.AiParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiParseServiceImpl implements AiParseService {

    private static final int STATUS_PENDING = 0;

    private final AiParseTaskMapper parseTaskMapper;

    @Override
    @Transactional
    public ParseTaskVO submitParseTask(ParseTaskRequest request) {
        if (request == null || request.getResumeId() == null || request.getAttachmentId() == null) {
            throw new IllegalArgumentException("resumeId 与 attachmentId 不能为空");
        }
        AiParseTask task = new AiParseTask();
        task.setResumeId(request.getResumeId());
        task.setAttachmentId(request.getAttachmentId());
        task.setModelId(request.getModelId());
        task.setTaskStatus(STATUS_PENDING);
        parseTaskMapper.insert(task);
        return toVO(task);
    }

    @Override
    public ParseTaskVO getParseTask(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId 不能为空");
        }
        AiParseTask task = parseTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("解析任务不存在");
        }
        return toVO(task);
    }

    @Override
    public ParseTaskVO getLatestByResumeId(Long resumeId) {
        if (resumeId == null) {
            throw new IllegalArgumentException("resumeId 不能为空");
        }
        AiParseTask task = parseTaskMapper.selectOne(
                new LambdaQueryWrapper<AiParseTask>()
                        .eq(AiParseTask::getResumeId, resumeId)
                        .orderByDesc(AiParseTask::getCreatedAt)
                        .last("LIMIT 1"),
                false);
        return task == null ? null : toVO(task);
    }

    private ParseTaskVO toVO(AiParseTask task) {
        ParseTaskVO vo = new ParseTaskVO();
        vo.setTaskId(task.getId());
        vo.setResumeId(task.getResumeId());
        vo.setAttachmentId(task.getAttachmentId());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setErrorMessage(task.getErrorMessage());
        vo.setStartedAt(task.getStartedAt());
        vo.setFinishedAt(task.getFinishedAt());
        return vo;
    }
}
