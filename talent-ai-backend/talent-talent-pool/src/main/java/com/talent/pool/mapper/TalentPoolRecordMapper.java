package com.talent.pool.mapper;

import com.talent.pool.entity.TalentPoolRecord;
import com.talent.pool.vo.TalentPoolRecordVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 人才库记录表 Mapper 接口
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-17
 */
public interface TalentPoolRecordMapper extends BaseMapper<TalentPoolRecord> {

    /**
     * 分页查询人才库记录（含关联标签列表）
     *
     * @param page             分页参数
     * @param jobSeekingStatus 求职状态筛选（可选）
     * @param minScore         匹配分数下限（可选）
     * @param maxScore         匹配分数上限（可选）
     * @param tagId            标签ID筛选（可选）
     * @return 含标签列表的 VO 分页结果
     */
    List<TalentPoolRecordVO> selectPoolRecordPage(
            Page<TalentPoolRecordVO> page,
            @Param("jobSeekingStatus") Byte jobSeekingStatus,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            @Param("tagId") Long tagId
    );
}
