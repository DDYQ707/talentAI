package com.talent.admin.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页返回结构
 * <p>
 * 契合前端：{ records: List, total: long, page: int, size: int }
 *
 * @author TalentAI
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据列表 */
    private List<T> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int page;

    /** 每页条数 */
    private int size;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, int page, int size) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /**
     * 由 MyBatis-Plus 的 IPage 构建
     */
    public static <T> PageResult<T> of(IPage<T> p) {
        return new PageResult<>(p.getRecords(), p.getTotal(), (int) p.getCurrent(), (int) p.getSize());
    }
}