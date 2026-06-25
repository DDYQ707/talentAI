package com.talent.pool.controller;

import com.talent.common.api.R;
import com.talent.pool.dto.TalentPoolArchiveRequest;
import com.talent.pool.service.ITalentPoolRecordService;
import com.talent.pool.vo.TalentPoolRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 人才库内部接口（供微服务 Feign 调用）
 * <p>
 * 该控制器不直接面向前端，用于 talent-job 模块在候选人淘汰时
 * 跨服务调用归档逻辑。返回 Map 以兼容 Feign 客户端的通用签名。
 * </p>
 *
 * @author TalentAI
 * @since 2026-06-22
 */
@RestController
@RequestMapping("/talent-pool/internal")
public class TalentPoolInternalController {

    @Autowired
    private ITalentPoolRecordService talentPoolRecordService;

    /**
     * 内部调用：将候选人归档至人才库
     * <p>
     * 由 talent-job 模块在候选人淘汰时通过 Feign 调用。
     * 复用已有的 archive 逻辑，将 R 结果转为 Map 返回。
     * </p>
     */
    @PostMapping("/archive")
    public Map<String, Object> archiveFromHook(@RequestBody TalentPoolArchiveRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            R<TalentPoolRecordVO> r = talentPoolRecordService.archive(request, null);
            result.put("code", r.getCode());
            result.put("msg", r.getMsg());
            if (r.getData() != null) {
                result.put("data", r.getData());
            }
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "归档异常：" + e.getMessage());
        }
        return result;
    }
}
