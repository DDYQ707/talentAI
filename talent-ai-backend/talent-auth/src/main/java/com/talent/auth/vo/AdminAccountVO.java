package com.talent.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAccountVO {

    private Long id;
    private String account;
    private String phone;
    private String email;
    private String nickname;
    private Byte userType;
    private String userTypeLabel;
    private Byte status;
    private String statusLabel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
