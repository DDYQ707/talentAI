package com.talent.analytics.feign.dto;

import lombok.Data;

/** 月度统计数据（Feign反序列化） */
@Data
public class MonthlyCountDTO {
    private String yearMonth;
    private Long count;
}
