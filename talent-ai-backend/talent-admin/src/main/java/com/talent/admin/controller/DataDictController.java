package com.talent.admin.controller;

import com.talent.admin.common.Result;
import com.talent.admin.entity.DictItem;
import com.talent.admin.entity.DictType;
import com.talent.admin.service.IDataDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据字典 控制器
 *
 * @author TalentAI
 */
@RestController
@RequestMapping("/api/admin/dict")
@RequiredArgsConstructor
public class DataDictController {

    private final IDataDictService dataDictService;

    // ===== 字典类型 =====

    /** 获取所有字典类型列表 */
    @GetMapping("/types")
    public Result<List<DictType>> listTypes() {
        return Result.success(dataDictService.listTypes());
    }

    /** 新增字典类型 */
    @PostMapping("/types")
    public Result<DictType> createType(@RequestBody DictType dictType) {
        return Result.success(dataDictService.createType(dictType));
    }

    /** 更新字典类型 */
    @PutMapping("/types/{id}")
    public Result<Void> updateType(@PathVariable Long id, @RequestBody DictType dictType) {
        dataDictService.updateType(id, dictType);
        return Result.success();
    }

    /** 删除字典类型 */
    @DeleteMapping("/types/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        dataDictService.deleteType(id);
        return Result.success();
    }

    // ===== 字典项 =====

    /** 根据 typeId 获取字典项，按 sort 升序 */
    @GetMapping("/items")
    public Result<List<DictItem>> listItems(@RequestParam Long typeId) {
        return Result.success(dataDictService.listItems(typeId));
    }

    /** 新增字典项 */
    @PostMapping("/items")
    public Result<DictItem> createItem(@RequestBody DictItem dictItem) {
        return Result.success(dataDictService.createItem(dictItem));
    }

    /** 更新字典项 */
    @PutMapping("/items/{id}")
    public Result<Void> updateItem(@PathVariable Long id, @RequestBody DictItem dictItem) {
        dataDictService.updateItem(id, dictItem);
        return Result.success();
    }

    /** 删除字典项 */
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        dataDictService.deleteItem(id);
        return Result.success();
    }
}