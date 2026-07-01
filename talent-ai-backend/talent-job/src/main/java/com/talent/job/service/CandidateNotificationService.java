package com.talent.job.service;

import com.talent.job.entity.JobApplication;
import com.talent.job.entity.Offer;

public interface CandidateNotificationService {

    void notifyApplicationSubmitted(JobApplication application);

    void notifyScreenStatusChanged(JobApplication application, int screenStatus);

    void notifyOfferIssued(JobApplication application, Offer offer);

    void notifyOfferAccepted(JobApplication application, Offer offer);

    void notifyOfferDeclined(JobApplication application, Offer offer);
}
