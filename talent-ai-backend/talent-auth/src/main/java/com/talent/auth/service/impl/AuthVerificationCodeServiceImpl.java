package com.talent.auth.service.impl;

import com.talent.auth.entity.AuthVerificationCode;
import com.talent.auth.mapper.AuthVerificationCodeMapper;
import com.talent.auth.service.IAuthVerificationCodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 验证码表 服务实现类
 * </p>
 *
 * @author TalentAI
 * @since 2026-05-19
 */
@Service
public class AuthVerificationCodeServiceImpl extends ServiceImpl<AuthVerificationCodeMapper, AuthVerificationCode> implements IAuthVerificationCodeService {

}
