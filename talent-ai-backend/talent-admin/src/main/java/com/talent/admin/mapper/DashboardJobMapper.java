package com.talent.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 大屏 - job 库统计 Mapper（@DS("job")）
 *
 * @author TalentAI
 */
@Mapper
@DS("job")
public interface DashboardJobMapper {

    /** 今日投递峰值 */
    @Select("SELECT COUNT(*) FROM job_application WHERE is_deleted = 0 AND DATE(applied_at) = CURDATE()")
    long countTodayDelivery();

    /** 近30天投递按天分组（key: d=日期, c=数量） */
    @Select("SELECT DATE(applied_at) AS d, COUNT(*) AS c FROM job_application " +
            "WHERE is_deleted = 0 AND applied_at >= #{start} GROUP BY DATE(applied_at)")
    List<Map<String, Object>> deliveriesByDay(@Param("start") LocalDateTime start);

    /** 近30天岗位发布按天分组（key: d=日期, c=数量） */
    @Select("SELECT DATE(created_at) AS d, COUNT(*) AS c FROM job_post " +
            "WHERE is_deleted = 0 AND created_at >= #{start} GROUP BY DATE(created_at)")
    List<Map<String, Object>> publicationsByDay(@Param("start") LocalDateTime start);

    /** 行业分布：按工作城市分组 Top N（key: industry=城市, value=数量） */
    @Select("SELECT COALESCE(NULLIF(work_city, ''), '其他') AS industry, COUNT(*) AS value " +
            "FROM job_post WHERE is_deleted = 0 " +
            "GROUP BY COALESCE(NULLIF(work_city, ''), '其他') ORDER BY value DESC LIMIT 8")
    List<Map<String, Object>> industryByCity();
}