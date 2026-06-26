package com.talent.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 大屏 - auth 库统计 Mapper（@DS("auth")）
 *
 * @author TalentAI
 */
@Mapper
@DS("auth")
public interface DashboardAuthMapper {

    /** 总用户数 */
    @Select("SELECT COUNT(*) FROM sys_user WHERE is_deleted = 0")
    long countUsers();
}