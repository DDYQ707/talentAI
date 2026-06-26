package com.talent.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 大屏 - master 库统计 Mapper（@DS("master")）
 *
 * @author TalentAI
 */
@Mapper
@DS("master")
public interface DashboardMasterMapper {

    /** 入驻企业数（审核通过 status=1） */
    @Select("SELECT COUNT(*) FROM talent_enterprise_audit WHERE status = 1")
    long countEnterprises();

    /** AI 风控拦截数（status=1 风险预警） */
    @Select("SELECT COUNT(*) FROM talent_job_risk WHERE status = 1")
    long countRiskBlocked();
}