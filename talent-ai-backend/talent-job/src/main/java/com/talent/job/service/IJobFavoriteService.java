package com.talent.job.service;

import java.util.Map;

public interface IJobFavoriteService {

    Map<String, Object> toggleFavorite(Long candidateId, String userRole, Long jobId);

    Map<String, Object> listFavoriteJobIds(Long candidateId, String userRole);

    Map<String, Object> listMyFavorites(Long candidateId, String userRole, Integer current, Integer size);
}
