package com.talent.auth.vo;

import java.util.List;
import lombok.Data;

@Data
public class CandidateProfileCompletenessVO {

    private boolean complete;
    private byte completeness;
    private List<String> missingFields;
}
