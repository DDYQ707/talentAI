package com.talent.agent.domain.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewNoteSynthesizeRequest {

    private Long interviewId;

    /** 可选：前端传入最新笔记，不传则使用已保存内容 */
    private String noteContent;
}
