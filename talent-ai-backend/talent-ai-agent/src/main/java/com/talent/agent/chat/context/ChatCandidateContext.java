package com.talent.agent.chat.context;

import com.talent.agent.domain.vo.ChatCandidateCardVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatCandidateContext {

    private final ThreadLocal<List<ChatCandidateCardVO>> candidates = ThreadLocal.withInitial(ArrayList::new);

    public void clear() {
        candidates.get().clear();
    }

    public void remove() {
        candidates.remove();
    }

    public List<ChatCandidateCardVO> snapshot() {
        return List.copyOf(candidates.get());
    }

    public void addFromResumeRecords(List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (Map<String, Object> record : records) {
            addFromResumeRecord(record);
        }
    }

    public void addFromResumeRecord(Map<String, Object> record) {
        if (record == null || record.isEmpty()) {
            return;
        }
        ChatCandidateCardVO card = toCard(record);
        if (card.getResumeId() == null) {
            return;
        }
        List<ChatCandidateCardVO> list = candidates.get();
        boolean exists = list.stream().anyMatch(item -> card.getResumeId().equals(item.getResumeId()));
        if (!exists) {
            list.add(card);
        }
    }

    public void enrichMatch(Long resumeId, Long applicationId, Integer matchScore) {
        if (resumeId == null) {
            return;
        }
        for (ChatCandidateCardVO card : candidates.get()) {
            if (resumeId.equals(card.getResumeId())) {
                if (applicationId != null) {
                    card.setApplicationId(applicationId);
                }
                if (matchScore != null) {
                    card.setMatchScore(matchScore);
                }
                return;
            }
        }
        ChatCandidateCardVO card = new ChatCandidateCardVO();
        card.setResumeId(resumeId);
        card.setApplicationId(applicationId);
        card.setMatchScore(matchScore);
        candidates.get().add(card);
    }

    private ChatCandidateCardVO toCard(Map<String, Object> record) {
        ChatCandidateCardVO card = new ChatCandidateCardVO();
        card.setResumeId(asLong(record.get("id")));
        card.setCandidateId(asLong(record.get("candidateId")));
        card.setCandidateName(asString(record.get("candidateName")));
        card.setResumeName(asString(record.get("resumeName")));
        card.setCurrentTitle(asString(record.get("currentTitle")));
        card.setCity(asString(record.get("city")));
        card.setAppliedJobTitle(asString(record.get("appliedJobTitle")));
        card.setMatchScore(asInteger(record.get("matchScore")));
        card.setScreenStatus(asInteger(record.get("screenStatus")));
        card.setAttachmentId(asLong(record.get("attachmentId")));
        return card;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private String asString(Object value) {
        return value != null && StringUtils.hasText(String.valueOf(value))
                ? String.valueOf(value).trim()
                : null;
    }
}
