package com.talent.resume.dto;

import java.math.BigDecimal;
import lombok.Data;

/** 调用 auth 服务回填候选人档案（仅补空字段） */
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
