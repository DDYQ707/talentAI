package com.talent.agent.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedInterviewQuestionsDto {

    private List<QuestionItem> questions;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionItem {

        private String questionText;

        private String category;

        private String focusPoint;
    }
}
