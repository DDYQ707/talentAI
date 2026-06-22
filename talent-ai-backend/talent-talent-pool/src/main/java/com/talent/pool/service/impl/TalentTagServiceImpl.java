package com.talent.pool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.talent.common.api.R;
import com.talent.pool.dto.TalentTagCreateRequest;
import com.talent.pool.entity.TalentTag;
import com.talent.pool.mapper.TalentTagMapper;
import com.talent.pool.service.ITalentTagService;
import com.talent.pool.vo.TalentTagVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 人才标签字典表 服务实现类
 *
 * @author TalentAI
 * @since 2026-06-17
 */
@Service
public class TalentTagServiceImpl extends ServiceImpl<TalentTagMapper, TalentTag> implements ITalentTagService {

    @Override
    public R<TalentTagVO> createTag(TalentTagCreateRequest req) {
        if (req == null || !StringUtils.hasText(req.getTagName())) {
            return R.fail("标签名不能为空");
        }

        // 检查标签名唯一
        long existCount = count(new LambdaQueryWrapper<TalentTag>()
                .eq(TalentTag::getTagName, req.getTagName().trim()));
        if (existCount > 0) {
            return R.fail("标签名已存在");
        }

        TalentTag tag = new TalentTag();
        tag.setTagName(req.getTagName().trim());
        tag.setTagType(req.getTagType() != null ? req.getTagType() : (byte) 1);

        if (!save(tag)) {
            return R.fail("标签创建失败");
        }

        TalentTagVO vo = new TalentTagVO();
        vo.setId(tag.getId());
        vo.setTagName(tag.getTagName());
        vo.setTagType(tag.getTagType());
        return R.ok(vo);
    }

    @Override
    public R<List<TalentTagVO>> listAllTags() {
        List<TalentTag> tags = list(new LambdaQueryWrapper<TalentTag>()
                .orderByAsc(TalentTag::getTagType)
                .orderByAsc(TalentTag::getTagName));
        List<TalentTagVO> voList = tags.stream().map(tag -> {
            TalentTagVO vo = new TalentTagVO();
            vo.setId(tag.getId());
            vo.setTagName(tag.getTagName());
            vo.setTagType(tag.getTagType());
            return vo;
        }).collect(Collectors.toList());
        return R.ok(voList);
    }
}
