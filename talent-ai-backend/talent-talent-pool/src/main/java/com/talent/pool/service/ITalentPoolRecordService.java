package com.talent.pool.service;

import com.talent.common.api.R;
import com.talent.pool.dto.TagBindRequest;
import com.talent.pool.dto.TalentPoolArchiveRequest;
import com.talent.pool.dto.TalentPoolQueryRequest;
import com.talent.pool.dto.TalentPoolUpdateRequest;
import com.talent.pool.entity.TalentPoolRecord;
import com.talent.pool.vo.TalentPoolRecordVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * <p>
 * 人才库记录表 服务类
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-17
 */
public interface ITalentPoolRecordService extends IService<TalentPoolRecord> {

    /**
     * 人才归档入库
     */
    R<TalentPoolRecordVO> archive(TalentPoolArchiveRequest req, Long operatorId);

    /**
     * 逻辑删除归档记录
     */
    R<Void> deleteRecord(Long id);

    /**
     * 更新归档记录
     */
    R<Void> updateRecord(Long id, TalentPoolUpdateRequest req);

    /**
     * 为人才库记录批量绑定标签
     */
    R<Void> bindTags(Long poolRecordId, TagBindRequest req);

    /**
     * 移除人才库记录的某个标签
     */
    R<Void> unbindTag(Long poolRecordId, Long tagId);

    /**
     * 人才大厅分页查询（含标签列表）
     */
    R<Map<String, Object>> listPage(TalentPoolQueryRequest req);
}
