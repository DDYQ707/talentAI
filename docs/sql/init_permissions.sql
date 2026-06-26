USE talent_auth_db;

-- ============ 1. 初始化权限项（按模块分组，20 条）============
-- perm_type: 2=操作；parent_id 这里简化为 0（不做目录层级）
INSERT INTO `sys_permission` (`id`, `parent_id`, `perm_code`, `perm_name`, `module_name`, `perm_type`, `sort_order`) VALUES
-- 招聘管理
(1,  0, 'job:view',     '查看岗位',     '招聘管理', 2, 1),
(2,  0, 'job:create',   '发布岗位',     '招聘管理', 2, 2),
(3,  0, 'job:edit',     '编辑岗位',     '招聘管理', 2, 3),
(4,  0, 'job:close',    '关闭岗位',     '招聘管理', 2, 4),
-- 简历管理
(5,  0, 'resume:view',     '查看简历',  '简历管理', 2, 1),
(6,  0, 'resume:download', '下载简历',  '简历管理', 2, 2),
(7,  0, 'resume:tag',      '标注简历',  '简历管理', 2, 3),
(8,  0, 'resume:delete',   '删除简历',  '简历管理', 2, 4),
-- AI 功能
(9,  0, 'ai:screen',    'AI初筛',      'AI功能', 2, 1),
(10, 0, 'ai:assistant', 'AI助手',      'AI功能', 2, 2),
(11, 0, 'ai:interview', 'AI面试官',    'AI功能', 2, 3),
(12, 0, 'ai:report',    'AI报告导出',  'AI功能', 2, 4),
-- 数据报表
(13, 0, 'report:view',   '查看报表',   '数据报表', 2, 1),
(14, 0, 'report:export', '导出报表',   '数据报表', 2, 2),
(15, 0, 'report:custom', '自定义报表', '数据报表', 2, 3),
-- 系统设置
(16, 0, 'sys:user',  '用户管理',     '系统设置', 2, 1),
(17, 0, 'sys:role',  '角色管理',     '系统设置', 2, 2),
(18, 0, 'sys:model', 'AI模型配置',   '系统设置', 2, 3),
(19, 0, 'sys:audit', '系统审计',     '系统设置', 2, 4);

-- ============ 2. 初始化角色-权限关联 ============
-- 超级管理员(1)：全部 19 项
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) 
SELECT 1, id FROM `sys_permission` WHERE is_deleted = 0;

-- HR管理员(2)：招聘全部 + 简历(除删除) + AI(初筛/助手/面试官) + 报表(查看/导出)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(2,1),(2,2),(2,3),(2,4),
(2,5),(2,6),(2,7),
(2,9),(2,10),(2,11),
(2,13),(2,14);

-- 面试官(3)：查看岗位 + 查看简历 + AI面试官 + 查看报表
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(3,1),(3,5),(3,11),(3,13);

-- 部门负责人(4)：查看岗位 + 查看简历 + 报表全部
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(4,1),(4,5),(4,13),(4,14),(4,15);

-- 只读用户(5)：仅查看报表
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(5,13);