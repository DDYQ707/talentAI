package com.talent.admin.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 行业人才分布 VO
 *
 * @author TalentAI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IndustryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 行业 / 城市名 */
    private String industry;

    /** 数量 */
    private long value;
}