package com.talent.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

/** AI 解析结果回填候选人档案（微服务内部，仅补空字段） */
@Data
public class ParseProfilePatchRequest {

    private Long candidateId;

    private String realName;

    private String phone;

    private String email;

    private String city;

    private String currentTitle;

    private Byte highestEdu;

    private BigDecimal workYears;
}
