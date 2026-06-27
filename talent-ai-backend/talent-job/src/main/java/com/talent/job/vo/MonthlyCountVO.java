package com.talent.job.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 按月统计数据 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyCountVO {
    private String yearMonth;
    private Long count;
}
