package com.talent.agent.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatTurn {

    /** 1-user 2-assistant */
    private int role;

    private String content;
}
