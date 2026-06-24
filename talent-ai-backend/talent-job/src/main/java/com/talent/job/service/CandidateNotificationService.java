package com.talent.job.service;

import com.talent.job.entity.JobApplication;

public interface CandidateNotificationService {

    void notifyApplicationSubmitted(JobApplication application);

    void notifyScreenStatusChanged(JobApplication application, int screenStatus);
}
