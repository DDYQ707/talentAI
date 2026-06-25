package com.talent.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.talent.admin.entity.DictItem;
import com.talent.admin.entity.DictType;
import com.talent.admin.exception.BusinessException;
import com.talent.admin.mapper.DictItemMapper;
import com.talent.admin.mapper.DictTypeMapper;
import com.talent.admin.service.IDataDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据字典 服务实现
 *
 * @author TalentAI
 */
@Service
@RequiredArgsConstructor
public class DataDictServiceImpl implements IDataDictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    // ===== 字典类型 =====

    @Override
    public List<DictType> listTypes() {
        return dictTypeMapper.selectList(
                new LambdaQueryWrapper<DictType>().orderByDesc(DictType::getCreatedAt));
    }

    @Override
    public DictType createType(DictType dictType) {
        if (dictType == null || !StringUtils.hasText(dictType.getCode())) {
            throw new BusinessException(400, "字典编码不能为空");
        }
        if (!StringUtils.hasText(dictType.getName())) {
            throw new BusinessException(400, "字典名称不能为空");
        }
        long exist = dictTypeMapper.selectCount(
                new LambdaQueryWrapper<DictType>().eq(DictType::getCode, dictType.getCode()));
        if (exist > 0) {
            throw new BusinessException("字典编码已存在");
        }
        dictType.setId(null);
        if (dictType.getStatus() == null) {
            dictType.setStatus(1);
        }
        dictType.setCreatedAt(LocalDateTime.now());
        dictTypeMapper.insert(dictType);
        return dictType;
    }

    @Override
    public void updateType(Long id, DictType dictType) {
        DictType exist = requireType(id);
        // 编码变更需校验唯一
        if (StringUtils.hasText(dictType.getCode()) && !dictType.getCode().equals(exist.getCode())) {
            long c = dictTypeMapper.selectCount(
                    new LambdaQueryWrapper<DictType>().eq(DictType::getCode, dictType.getCode()));
            if (c > 0) {
                throw new BusinessException("字典编码已存在");
            }
        }
        dictType.setId(id);
        dictType.setCreatedAt(null);
        dictTypeMapper.updateById(dictType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteType(Long id) {
        requireType(id);
        // 级联清理字典项
        dictItemMapper.delete(new LambdaQueryWrapper<DictItem>().eq(DictItem::getDictTypeId, id));
        dictTypeMapper.deleteById(id);
    }

    // ===== 字典项 =====

    @Override
    public List<DictItem> listItems(Long typeId) {
        if (typeId == null) {
            throw new BusinessException(400, "typeId 不能为空");
        }
        return dictItemMapper.selectList(
                new LambdaQueryWrapper<DictItem>()
                        .eq(DictItem::getDictTypeId, typeId)
                        .orderByAsc(DictItem::getSort));
    }

    @Override
    public DictItem createItem(DictItem dictItem) {
        if (dictItem == null || dictItem.getDictTypeId() == null) {
            throw new BusinessException(400, "关联字典类型ID不能为空");
        }
        requireType(dictItem.getDictTypeId());
        if (!StringUtils.hasText(dictItem.getLabel())) {
            throw new BusinessException(400, "字典标签不能为空");
        }
        if (!StringUtils.hasText(dictItem.getValue())) {
            throw new BusinessException(400, "字典键值不能为空");
        }
        dictItem.setId(null);
        if (dictItem.getSort() == null) {
            dictItem.setSort(0);
        }
        if (dictItem.getStatus() == null) {
            dictItem.setStatus(1);
        }
        dictItem.setCreatedAt(LocalDateTime.now());
        dictItemMapper.insert(dictItem);
        return dictItem;
    }

    @Override
    public void updateItem(Long id, DictItem dictItem) {
        requireItem(id);
        dictItem.setId(id);
        dictItem.setCreatedAt(null);
        dictItemMapper.updateById(dictItem);
    }

    @Override
    public void deleteItem(Long id) {
        requireItem(id);
        dictItemMapper.deleteById(id);
    }

    private DictType requireType(Long id) {
        if (id == null) {
            throw new BusinessException(400, "字典类型ID不能为空");
        }
        DictType type = dictTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException(404, "字典类型不存在");
        }
        return type;
    }

    private DictItem requireItem(Long id) {
        if (id == null) {
            throw new BusinessException(400, "字典项ID不能为空");
        }
        DictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "字典项不存在");
        }
        return item;
    }
}