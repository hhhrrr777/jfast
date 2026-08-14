package io.github.hhhrrr777.jfast.templates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.hhhrrr777.jfast.core.FileTreeWalker;
import io.github.hhhrrr777.jfast.core.FreemarkerTemplateEngine;
import io.github.hhhrrr777.jfast.preset.Preset;
import io.github.hhhrrr777.jfast.preset.PresetLoader;
import io.github.hhhrrr777.jfast.wizard.Answers;
import io.github.hhhrrr777.jfast.wizard.QuestionId;
import io.github.hhhrrr777.jfast.wizard.Questionnaire;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * S1-4 验收:base 前端模板树渲染后 `npm install && npm run build` 通过。
 * 本测试需要 Node ^20.19 或 >=22.12;本地未装 Node 时跳过。
 *
 * base 层现含前后端模板,渲染模型走真实 Questionnaire.toConfiguration(...).toRenderModel()
 * (S1-2 契约),不手工伪造,确保前后端模板引用的键齐全。
 */
class BaseFrontendBuildTest {

    private final FileTreeWalker walker = new FileTreeWalker(new FreemarkerTemplateEngine());
    private final PresetLoader presetLoader = new PresetLoader();

    @TempDir
    Path outputDir;

    /** 经 S1-2 契约构造真实渲染模型;systemAdmin 决定走 full 还是 empty 预设。 */
    private Map<String, Object> model(boolean systemAdmin) {
        Preset preset = presetLoader.load(systemAdmin ? "full" : "empty");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);
        Answers answers = Answers.builder()
                .set(QuestionId.GROUP_ID, "com.example")
                .set(QuestionId.ARTIFACT_ID, "demo-app")
                .set(QuestionId.BASE_PACKAGE, "com.example.demo")
                .set(QuestionId.JDK_VERSION, "21")
                .set(QuestionId.DATABASE, "mysql")
                .set(QuestionId.DB_HOST, "localhost")
                .set(QuestionId.DB_PORT, "3306")
                .set(QuestionId.DB_NAME, "demo_app")
                .set(QuestionId.DB_USER, "root")
                .set(QuestionId.DB_PASSWORD, "password123")
                .set(QuestionId.SERVER_PORT, "8080")
                .build();
        return questionnaire.toConfiguration(answers, "unused").toRenderModel();
    }

    @Test
    void emptyPresetRendersMinimalShellWithoutAdmin() throws Exception {
        walker.generate(List.of("templates/base"), outputDir, model(false));

        String app = Files.readString(outputDir.resolve("src/App.vue"), StandardCharsets.UTF_8);
        String form = Files.readString(outputDir.resolve("src/views/entity/form.vue"), StandardCharsets.UTF_8);

        assertThat(app).contains("simple-main").doesNotContain("admin-header");
        assertThat(form).doesNotContain("el-page-header").doesNotContain("el-form");
        assertThat(form).contains("title");
    }

    @Test
    void fullAdminPresetRendersAdminShell() throws Exception {
        walker.generate(List.of("templates/base"), outputDir, model(true));

        String app = Files.readString(outputDir.resolve("src/App.vue"), StandardCharsets.UTF_8);
        String form = Files.readString(outputDir.resolve("src/views/entity/form.vue"), StandardCharsets.UTF_8);

        assertThat(app).contains("admin-header").contains("admin-main");
        assertThat(form).contains("el-page-header").contains("el-form");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    @EnabledOnOs({OS.MAC, OS.LINUX})
    void rendersBaseFrontendAndBuildsBothPresetBranches() throws Exception {
        assumeNodeAvailable();

        for (boolean systemAdmin : new boolean[]{false, true}) {
            Path presetDir = outputDir.resolve(systemAdmin ? "full" : "empty");
            walker.generate(List.of("templates/base"), presetDir, model(systemAdmin));

            assertThat(presetDir.resolve("package.json")).exists();
            assertThat(presetDir.resolve(".gitignore")).exists();
            assertThat(presetDir.resolve("vite.config.ts")).exists();
            assertThat(presetDir.resolve("src/main.ts")).exists();
            assertThat(presetDir.resolve("src/router/index.ts")).exists();
            assertThat(presetDir.resolve("src/utils/request.ts")).exists();
            assertThat(presetDir.resolve("src/views/entity/form.vue")).exists();

            run(presetDir, "npm", "install");
            run(presetDir, "npm", "run", "build");

            assertThat(presetDir.resolve("dist")).isDirectory();
            assertThat(presetDir.resolve("dist/index.html")).isRegularFile();
        }
    }

    private void assumeNodeAvailable() throws Exception {
        boolean available = false;
        try {
            Process process = new ProcessBuilder("node", "--version").start();
            String version = new String(process.getInputStream().readAllBytes()).strip();
            process.waitFor(5, TimeUnit.SECONDS);
            available = process.exitValue() == 0 && matchesRequiredNode(version);
        } catch (Exception ignored) {
        }
        assumeTrue(available, "需要 Node ^20.19 或 >=22.12");
    }

    private boolean matchesRequiredNode(String version) {
        if (version.startsWith("v")) {
            version = version.substring(1);
        }
        int[] parts = parseVersion(version);
        if (parts == null) {
            return false;
        }
        int major = parts[0];
        int minor = parts[1];
        return (major == 20 && minor >= 19) || major >= 22;
    }

    private int[] parseVersion(String version) {
        String[] split = version.split("\\.");
        if (split.length < 2) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(split[0]), Integer.parseInt(split[1])};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void run(Path workingDir, String... command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDir.toFile());
        builder.redirectErrorStream(true);
        Path logFile = workingDir.resolve(".jfast-" + command[0] + "-" + command[command.length - 1] + ".log");
        builder.redirectOutput(logFile.toFile());
        Process process = builder.start();
        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
        }
        String output = Files.readString(logFile, StandardCharsets.UTF_8);
        assertThat(finished)
                .as("命令应在超时前完成: %s%n%s", String.join(" ", command), output)
                .isTrue();
        assertThat(process.exitValue())
                .as("命令应成功退出: %s%n%s", String.join(" ", command), output)
                .isZero();
    }
}
