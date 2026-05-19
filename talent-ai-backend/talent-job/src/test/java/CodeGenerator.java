
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        // 1. 注意这里已经是 talent_job_db 了
        String url = "jdbc:mysql://127.0.0.1:3306/talent_job_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "dyq!"; // <--- 【必改】

        // 2. 你的 talent-job 模块所在的绝对路径
        String projectPath = "C:\\Users\\32805\\Desktop\\智能招聘与人才画像分析系统\\talent-ai-system\\talent-ai-backend\\talent-job"; // <--- 【必改】

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder.author("TalentAI")
                            .outputDir(projectPath + "/src/main/java");
                })
                .packageConfig(builder -> {
                    builder.parent("com.talent.job") // 父包名为 com.talent.job
                            .pathInfo(Collections.singletonMap(OutputFile.xml, projectPath + "/src/main/resources/mapper"));
                })
                .strategyConfig(builder -> {
                    // 只生成这三张表
                    builder.addInclude("job_post", "job_application", "application_stage_log")
                            .entityBuilder()
                            .enableLombok()
                            .logicDeleteColumnName("is_deleted")
                            .controllerBuilder()
                            .enableRestStyle();
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();

        System.out.println("🎉 talent-job 代码生成完毕！");
    }
}