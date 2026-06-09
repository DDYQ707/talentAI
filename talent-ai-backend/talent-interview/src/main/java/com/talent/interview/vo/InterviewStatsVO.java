package com.talent.interview.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InterviewStatsVO {

    private long todayPending;

    private long weekTotal;

    private long completed;

    private long toSchedule;

    private long cancelled;
}
