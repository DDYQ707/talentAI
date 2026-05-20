package com.talent.auth.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CandidateProfileVO {

    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private String avatarUrl;

    private String realName;
    private Byte gender;
    private String birthDate;
    private String currentTitle;
    private BigDecimal workYears;
    private Byte highestEdu;
    private String city;
    private Byte jobSeekingStatus;
    private Byte resumeCompleteness;
    private Byte aiScore;

    /** 是否满足投递前的必填项 */
    private Boolean profileComplete;
}
