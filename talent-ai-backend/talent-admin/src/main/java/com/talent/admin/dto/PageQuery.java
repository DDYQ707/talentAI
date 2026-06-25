package com.talent.admin.dto;

import lombok.Data;

/**
 * 通用分页查询参数
 *
 * @author TalentAI
 */
@Data
public class PageQuery {

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页条数 */
    private Integer size = 10;
}