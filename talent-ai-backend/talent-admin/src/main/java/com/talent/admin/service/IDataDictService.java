package com.talent.admin.service;

import com.talent.admin.entity.DictItem;
import com.talent.admin.entity.DictType;

import java.util.List;

/**
 * 数据字典 服务接口
 *
 * @author TalentAI
 */
public interface IDataDictService {

    // ===== 字典类型 =====

    /** 获取所有字典类型列表 */
    List<DictType> listTypes();

    /** 新增字典类型 */
    DictType createType(DictType dictType);

    /** 更新字典类型 */
    void updateType(Long id, DictType dictType);

    /** 删除字典类型（同时清理其下字典项） */
    void deleteType(Long id);

    // ===== 字典项 =====

    /** 根据 typeId 获取字典项，按 sort 升序 */
    List<DictItem> listItems(Long typeId);

    /** 新增字典项 */
    DictItem createItem(DictItem dictItem);

    /** 更新字典项 */
    void updateItem(Long id, DictItem dictItem);

    /** 删除字典项 */
    void deleteItem(Long id);
}