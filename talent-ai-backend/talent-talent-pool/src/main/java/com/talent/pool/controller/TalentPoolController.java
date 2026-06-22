package com.talent.pool.controller;

import com.talent.common.api.R;
import com.talent.pool.dto.TagBindRequest;
import com.talent.pool.dto.TalentPoolArchiveRequest;
import com.talent.pool.dto.TalentPoolQueryRequest;
import com.talent.pool.dto.TalentPoolUpdateRequest;
import com.talent.pool.dto.TalentTagCreateRequest;
import com.talent.pool.service.ITalentPoolRecordService;
import com.talent.pool.service.ITalentTagService;
import com.talent.pool.vo.TalentPoolRecordVO;
import com.talent.pool.vo.TalentTagVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 人才库 前端控制器
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-17
 */
@RestController
@RequestMapping("/api/talent-pool")
public class TalentPoolController {

    @Autowired
    private ITalentPoolRecordService talentPoolRecordService;

    @Autowired
    private ITalentTagService talentTagService;

    // ======================== 人才归档 CRUD ========================

    /**
     * 人才归档入库
     */
    @PostMapping("/archive")
    public R<TalentPoolRecordVO> archive(
            @RequestBody TalentPoolArchiveRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return talentPoolRecordService.archive(request, userId);
    }

    /**
     * 删除归档记录（逻辑删除）
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteRecord(@PathVariable Long id) {
        return talentPoolRecordService.deleteRecord(id);
    }

    /**
     * 更新归档记录
     */
    @PutMapping("/{id}")
    public R<Void> updateRecord(
            @PathVariable Long id,
            @RequestBody TalentPoolUpdateRequest request) {
        return talentPoolRecordService.updateRecord(id, request);
    }

    // ======================== 标签管理 ========================

    /**
     * 为人才库记录批量绑定标签
     */
    @PostMapping("/{poolRecordId}/tags")
    public R<Void> bindTags(
            @PathVariable Long poolRecordId,
            @RequestBody TagBindRequest request) {
        return talentPoolRecordService.bindTags(poolRecordId, request);
    }

    /**
     * 移除人才库记录的某个标签
     */
    @DeleteMapping("/{poolRecordId}/tags/{tagId}")
    public R<Void> unbindTag(
            @PathVariable Long poolRecordId,
            @PathVariable Long tagId) {
        return talentPoolRecordService.unbindTag(poolRecordId, tagId);
    }

    /**
     * 创建标签
     */
    @PostMapping("/tags")
    public R<TalentTagVO> createTag(@RequestBody TalentTagCreateRequest request) {
        return talentTagService.createTag(request);
    }

    /**
     * 查询所有标签列表
     */
    @GetMapping("/tags")
    public R<List<TalentTagVO>> listTags() {
        return talentTagService.listAllTags();
    }

    // ======================== 人才大厅查询 ========================

    /**
     * 人才大厅分页查询（支持求职状态、匹配分数筛选，返回含标签列表）
     */
    @GetMapping("/list")
    public R<Map<String, Object>> listPage(TalentPoolQueryRequest request) {
        return talentPoolRecordService.listPage(request);
    }
}
