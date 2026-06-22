package com.talent.agent.service;

import com.talent.agent.domain.dto.ProfileGenerateRequest;
import com.talent.agent.domain.vo.TalentProfileVO;

public interface AiTalentProfileService {

    TalentProfileVO generate(ProfileGenerateRequest request);

    TalentProfileVO getByApplicationId(Long applicationId);
}
