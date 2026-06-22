package com.talent.auth.test; // 根据你的实际包名调整

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

public class CodeGenerator {

    public static void main(String[] args) {

        // 1. 数据库连接配置 (已经帮你写好了刚才确认的 3306 端口和 talent_auth_db 库名)
        String url = "jdbc:mysql://127.0.0.1:3307/talent_auth_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "dyq!"; // <--- 【必改】填你的 MySQL 密码

        // 2. 你的 talent-auth 模块所在的绝对路径
        // <--- 【必改】右键你的 talent-auth 文件夹，Copy Path/Reference -> Absolute Path，粘贴到这里
        String projectPath = "C:\\Users\\32805\\Desktop\\智能招聘与人才画像分析系统\\talent-ai-system\\talent-ai-backend\\talent-auth";

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder.author("TalentAI") // 作者名字
                            .outputDir(projectPath + "/src/main/java"); // 指定 java 代码输出目录
                })
                .packageConfig(builder -> {
                    builder.parent("com.talent.auth") // 父包名，会生成 com.talent.auth.entity 等
                            // 设置 mapper xml 的输出路径到 resources/mapper 目录下
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper"));
                })
                .strategyConfig(builder -> {
                    // 指定刚才建的 8 张表
                    builder.addInclude(
                                    "sys_user", "sys_role", "sys_permission",
                                    "sys_user_role", "sys_role_permission",
                                    "sys_department", "auth_verification_code", "candidate_profile"
                            )
                            // 实体类配置：开启 lombok，并指定逻辑删除字段
                            .entityBuilder()
                            .enableLombok()
                            .logicDeleteColumnName("is_deleted")
                            // Controller 配置：开启 @RestController
                            .controllerBuilder()
                            .enableRestStyle();
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        System.out.println("🎉 代码生成完毕！请刷新 IDEA 目录树！");
    }
}