package com.talent.admin.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 供需趋势单点 VO
 *
 * @author TalentAI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日期 MM-dd */
    private String date;

    /** 当日简历投递数 */
    private long resumeDeliveries;

    /** 当日岗位发布数 */
    private long jobPublications;
}