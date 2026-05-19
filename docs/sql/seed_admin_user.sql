-- 开发环境默认管理员：用户名 admin，密码 123456
-- 执行前请确认已创建 talent_ai 库及 sys_user 表（见 talent_ai_schema.sql）

DELETE FROM `sys_user` WHERE `username` = 'admin' AND `is_deleted` = 0;

INSERT INTO `sys_user` (
  `username`,
  `password_hash`,
  `nickname`,
  `user_type`,
  `status`
) VALUES (
  'admin',
  '$2b$10$AeGqsj1Ysp4c9FrTo26KZu1fkS49Vdt.SduMikjS68WuY0hmvmIna',
  '系统管理员',
  4,
  1
);
