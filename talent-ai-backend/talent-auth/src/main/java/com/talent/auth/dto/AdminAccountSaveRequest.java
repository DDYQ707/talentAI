package com.talent.auth.dto;

import lombok.Data;

/**
 * 管理员创建/更新 HR、面试官账号
 */
@Data
public class AdminAccountSaveRequest {

    /** 手机号或邮箱 */
    private String account;

    /** 初始密码（创建必填；更新时留空表示不修改） */
    private String password;

    /** 2-HR 3-面试官 */
    private Byte userType;

    /** 显示昵称，可选 */
    private String nickname;

    /** 账号状态：0-禁用 1-正常，更新时使用 */
    private Byte status;
}
