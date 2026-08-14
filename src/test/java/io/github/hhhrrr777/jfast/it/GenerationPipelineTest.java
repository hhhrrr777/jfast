package io.github.hhhrrr777.jfast.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 生成管线集成测试(ADR-0009 T2):以全参数模式在子进程端到端跑 CLI,
 * 断言退出码与生成物——文件清单快照(捕捉多了/少了文件)+ 关键文件定点断言
 * (捕捉渲染错变量)。
 *
 * empty 是本期唯一进入门禁矩阵的预设,先有此一条;full 模板化完成(S3-3)后在此扩展。
 * 快照更新:`mvn test -DupdateSnapshots`,diff 随 PR 人工 review。
 */
class GenerationPipelineTest {

    private static final String MAIN_CLASS =
            "src/main/java/com/example/demo/DemoApplication.java";

    @TempDir
    Path workDir;

    @Test
    void emptyPresetGeneratesExpectedProject() throws Exception {
        Path output = workDir.resolve("demo");
        RunResult result = runJfast("new",
                "--preset", "empty",
                "--group-id", "com.example",
                "--artifact-id", "demo",
                "--base-package", "com.example.demo",
                "--jdk-version", "21",
                "--database", "mysql",
                "--output-dir", output.toString());

        assertThat(result.exitCode())
                .as("jfast new 退出码非 0。\n--- stdout ---\n%s\n--- stderr ---\n%s",
                        result.stdout(), result.stderr())
                .isEqualTo(0);

        // 文件清单快照:多了/少了文件一眼可见
        FileListSnapshot.assertMatches("empty.filelist", output);

        // 定点断言:pom 版本坐标
        String pom = read(output.resolve("pom.xml"));
        assertThat(pom).as("pom.xml")
                .contains("<groupId>com.example</groupId>")
                .contains("<artifactId>demo</artifactId>")
                .contains("<version>3.5.16</version>")
                .contains("<mybatis-plus.version>3.5.17</mybatis-plus.version>")
                .contains("<jjwt.version>0.13.0</jjwt.version>");

        // 定点断言:主类(包路径 + 类名渲染)
        assertThat(read(output.resolve(MAIN_CLASS))).as(MAIN_CLASS)
                .startsWith("package com.example.demo;")
                .contains("class DemoApplication");

        // 定点断言:数据库驱动渲染(mysql 分叉)
        assertThat(read(output.resolve("src/main/resources/application.yml")))
                .as("application.yml")
                .contains("driver-class-name: com.mysql.cj.jdbc.Driver")
                .contains("jdbc:mysql://localhost:3306/demo");

        // 定点断言:条件渲染分叉点 conditions.systemAdmin——empty 预设为 false,
        // 系统管理块不得渲染:README 无数据库连接信息块、App.vue 无管理后台头部
        assertThat(read(output.resolve("README.md"))).as("README.md (systemAdmin=false)")
                .contains("数据库: mysql")
                .doesNotContain("数据库主机:")
                .doesNotContain("服务端口:");
        assertThat(read(output.resolve("src/App.vue"))).as("src/App.vue (systemAdmin=false)")
                .contains("simple-main")
                .doesNotContain("admin-header");

        // manifest 元数据(ADR-0004)不得拷入生成物
        assertThat(output.resolve("preset.yaml"))
                .as("preset.yaml 是 manifest 元数据,不应拷入生成物")
                .doesNotExist();
    }

    /** 以当前测试 classpath 子进程跑 jfast CLI(非 TTY,走全参数模式)。 */
    private RunResult runJfast(String... args) throws IOException, InterruptedException {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");

        List<String> command = new ArrayList<>(List.of(javaBin, "-cp", classpath, "io.github.hhhrrr777.jfast.cli.Main"));
        command.addAll(List.of(args));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(false)
                .start();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("jfast 子进程 60s 未结束: " + String.join(" ", command));
        }
        return new RunResult(process.exitValue(), stdout, stderr);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("读取生成物失败: " + file, e);
        }
    }

    private record RunResult(int exitCode, String stdout, String stderr) {
    }
}
