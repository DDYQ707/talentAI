package com.talent.admin.dto.dashboard;

import lombok.Data;

import java.io.Serializable;

/**
 * 微服务健康 VO
 *
 * @author TalentAI
 */
@Data
public class ServiceHealthVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 服务名 */
    private String name;

    /** 状态：UP / DOWN */
    private String status;

    /** 探测耗时(ms) */
    private long latency;

    /** 在线率 */
    private String uptime;

    /** 最后检查时间 */
    private String lastChecked;
}