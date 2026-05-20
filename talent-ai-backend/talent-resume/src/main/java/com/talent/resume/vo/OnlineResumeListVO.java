package com.talent.resume.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OnlineResumeListVO {

    private Long id;

    private String resumeName;

    private String summary;

    private Integer isDefault;

    private Integer completeness;

    private LocalDateTime updatedAt;
}
