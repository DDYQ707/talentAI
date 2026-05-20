package com.talent.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CandidateProfileSaveRequest {

    private String realName;
    private String phone;
    private String email;
    private Byte gender;
    private String birthDate;
    private String currentTitle;
    private BigDecimal workYears;
    private Byte highestEdu;
    private String city;
    private Byte jobSeekingStatus;
}
