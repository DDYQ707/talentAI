package com.talent.job.vo;

import com.talent.job.entity.JobPost;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobFavoriteVO {

    private Long favoriteId;

    private Long jobId;

    private LocalDateTime favoritedAt;

    private JobPost job;
}
