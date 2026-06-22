package com.talent.agent.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedTalentProfileDto {

    private String profileSummary;

    private List<String> profileTags;
}
