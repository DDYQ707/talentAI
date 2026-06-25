package com.talent.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 近7日调用趋势项
 *
 * @author TalentAI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageTrendVO {

    /** 日期，格式 MM-dd */
    private String date;

    /** 当日调用量 */
    private Long calls;
}