package com.talent.pool.service;

import com.talent.common.api.R;
import com.talent.pool.dto.TalentTagCreateRequest;
import com.talent.pool.entity.TalentTag;
import com.talent.pool.vo.TalentTagVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 人才标签字典表 服务类
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-17
 */
public interface ITalentTagService extends IService<TalentTag> {

    /**
     * 创建标签
     */
    R<TalentTagVO> createTag(TalentTagCreateRequest req);

    /**
     * 查询所有标签列表
     */
    R<List<TalentTagVO>> listAllTags();
}
