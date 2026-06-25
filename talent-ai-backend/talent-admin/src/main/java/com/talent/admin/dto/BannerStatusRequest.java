package com.talent.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 轮播图上下线状态更新请求
 *
 * @author TalentAI
 */
@Data
public class BannerStatusRequest {

    /** 状态：0=下线, 1=上线 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}