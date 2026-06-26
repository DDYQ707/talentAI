package com.talent.admin.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.talent.admin.dto.UsageDistVO;
import com.talent.admin.dto.UsageTrendVO;
import com.talent.admin.entity.AiModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 模型 Mapper —— 整库走 talent_ai_db（@DS("ai")）
 * <p>
 * 统计口径仅基于 ai_parse_task + ai_match_record 两张表。
 * 注：平均延迟仅取 ai_parse_task（旧库 ai_match_record 无 started_at / finished_at 字段）。
 *
 * @author TalentAI
 */
@DS("ai")
@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {

    /**
     * 今日调用量：两表 created_at 为今天的记录数之和。
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM ai_parse_task WHERE is_deleted = 0 AND DATE(created_at) = CURDATE())
            + (SELECT COUNT(*) FROM ai_match_record WHERE is_deleted = 0 AND DATE(created_at) = CURDATE())
            """)
    Long countTodayCalls();

    /**
     * Token 消耗总量：ai_match_record.token_used 求和（parse 无 token，按 0）。
     */
    @Select("SELECT IFNULL(SUM(token_used), 0) FROM ai_match_record WHERE is_deleted = 0")
    Long sumTokens();

    /**
     * 平均延迟（秒）：ai_parse_task started_at / finished_at 均非空记录的 TIMESTAMPDIFF 平均值。
     * 注：ai_match_record 旧库结构无 started_at / finished_at 字段，故不参与延迟统计。
     */
    @Select("""
            SELECT IFNULL(AVG(TIMESTAMPDIFF(SECOND, started_at, finished_at)), 0)
            FROM ai_parse_task
            WHERE is_deleted = 0 AND started_at IS NOT NULL AND finished_at IS NOT NULL
            """)
    BigDecimal avgLatencySeconds();

    /**
     * 活跃模型数：status=1 且未删除。
     */
    @Select("SELECT COUNT(*) FROM ai_model WHERE is_deleted = 0 AND status = 1")
    Long countActiveModels();

    /**
     * 近 N 天调用趋势：按日期分组（仅返回有数据的日期，Service 层补零）。
     * 返回 date 为 'MM-dd' 格式。
     *
     * @param startDate 起始日期（含），格式 yyyy-MM-dd
     */
    @Select("""
            SELECT d AS date, COUNT(*) AS calls FROM (
              SELECT DATE_FORMAT(created_at, '%m-%d') AS d
              FROM ai_parse_task
              WHERE is_deleted = 0 AND created_at >= #{startDate}
              UNION ALL
              SELECT DATE_FORMAT(created_at, '%m-%d') AS d
              FROM ai_match_record
              WHERE is_deleted = 0 AND created_at >= #{startDate}
            ) u
            GROUP BY d
            ORDER BY d
            """)
    List<UsageTrendVO> usageTrend(String startDate);

    /**
     * 各模型调用分布：按 model_id 分组统计两表调用次数，join ai_model 取名称。
     */
    @Select("""
            SELECT m.model_name AS name, IFNULL(s.cnt, 0) AS count
            FROM ai_model m
            LEFT JOIN (
              SELECT model_id, COUNT(*) AS cnt FROM (
                SELECT model_id FROM ai_parse_task WHERE is_deleted = 0 AND model_id IS NOT NULL
                UNION ALL
                SELECT model_id FROM ai_match_record WHERE is_deleted = 0 AND model_id IS NOT NULL
              ) c
              GROUP BY model_id
            ) s ON s.model_id = m.id
            WHERE m.is_deleted = 0
            ORDER BY count DESC
            """)
    List<UsageDistVO> usageDistribution();
}