package com.talent.job.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchCandidateIdsRequest {

    private List<Long> candidateIds;
}
