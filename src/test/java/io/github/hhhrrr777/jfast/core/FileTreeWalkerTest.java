package io.github.hhhrrr777.jfast.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.hhhrrr777.jfast.preset.PresetLoader;
import io.github.hhhrrr777.jfast.wizard.Answers;
import io.github.hhhrrr777.jfast.wizard.QuestionId;
import io.github.hhhrrr777.jfast.wizard.Questionnaire;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTreeWalkerTest {

    private final FileTreeWalker walker = new FileTreeWalker(new FreemarkerTemplateEngine());

    @TempDir
    Path outputDir;

    private static Map<String, Object> toyModel() {
        return Map.of("project", Map.of(
                "artifactId", "demo-app",
                "packageName", "com.example.demo",
                "packagePath", "com/example/demo"));
    }

    @Test
    void toyTreeRendersEndToEnd() throws Exception {
        walker.generate(List.of("fixtures/toy/base", "fixtures/toy/overlay"), outputDir, toyModel());

        // .ftl 渲染 + 剥后缀
        assertThat(outputDir.resolve("pom.xml")).hasContent("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project>
                  <artifactId>demo-app</artifactId>
                </project>
                """);

        // 文件名 ${} 与内容同模型同接缝
        assertThat(outputDir.resolve("src/main/java/com/example/demo/Main.java")).hasContent("""
                package com.example.demo;

                public class Main {
                    public static void main(String[] args) {
                        System.out.println("demo-app");
                    }
                }
                """);

        // ${'${'} 转义产出字面量;渲染文件中的模板变量照常替换
        assertThat(outputDir.resolve("src/main/resources/application.yml")).hasContent("""
                server:
                  port: ${server.port:8080}
                spring:
                  application:
                    name: demo-app
                """);

        // 无后缀文件字节级拷贝
        assertThat(outputDir.resolve(".gitignore")).hasContent("node_modules/\ndist/\n");
        assertThat(outputDir.resolve("static/logo.bin"))
                .hasBinaryContent(Files.readAllBytes(Path.of("src/test/resources/fixtures/toy/base/static/logo.bin")));

        // overlay 层新增文件
        assertThat(outputDir.resolve("README.md")).hasContent("# demo-app\n\n由 jfast 生成。\n");
    }

    @Test
    void baseOverlayCollisionFails() {
        assertThatThrownBy(() -> walker.generate(
                List.of("fixtures/collision/base", "fixtures/collision/overlay"), outputDir, toyModel()))
                .isInstanceOf(TemplateCollisionException.class)
                .hasMessageContaining("same.txt")
                .hasMessageContaining("fixtures/collision/base")
                .hasMessageContaining("fixtures/collision/overlay");
    }

    @Test
    void presetManifestIsNotCopiedIntoOutput() {
        // 经 S1-2 契约构造真实渲染模型,base 模板引用的键才不会缺
        Questionnaire questionnaire = Questionnaire.forPreset(new PresetLoader().load("empty"));
        Answers answers = Answers.builder()
                .set(QuestionId.GROUP_ID, "com.example")
                .set(QuestionId.ARTIFACT_ID, "demo")
                .set(QuestionId.BASE_PACKAGE, "com.example.demo")
                .set(QuestionId.JDK_VERSION, "21")
                .set(QuestionId.DATABASE, "mysql")
                .build();
        Map<String, Object> model = questionnaire.toConfiguration(answers, "unused").toRenderModel();

        walker.generate(List.of("templates/base", "templates/presets/empty"), outputDir, model);

        // preset.yaml 是 manifest 元数据(ADR-0004),walker 跳过不落盘
        assertThat(outputDir.resolve("preset.yaml")).doesNotExist();
        // base 模板照常生成
        assertThat(outputDir.resolve("pom.xml")).exists();
    }

    @Test
    void walksTemplatesInsideJar(@TempDir Path jarDir) throws Exception {
        Path jarPath = jarDir.resolve("templates.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jar.putNextEntry(new JarEntry("templates/base/"));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("templates/base/${project.artifactId}.txt.ftl"));
            jar.write("你好,${project.artifactId}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jar.closeEntry();
            jar.putNextEntry(new JarEntry("templates/base/raw.bin"));
            jar.write(new byte[]{0x00, 0x01, (byte) 0xff});
            jar.closeEntry();
        }

        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{jarPath.toUri().toURL()}, null)) {
            FileTreeWalker jarWalker = new FileTreeWalker(new FreemarkerTemplateEngine(), loader);
            jarWalker.generate(List.of("templates/base"), outputDir, toyModel());
        }

        assertThat(outputDir.resolve("demo-app.txt")).hasContent("你好,demo-app");
        assertThat(outputDir.resolve("raw.bin")).hasBinaryContent(new byte[]{0x00, 0x01, (byte) 0xff});
    }

    @Test
    void missingTemplateRootFailsWithClearMessage() {
        assertThatThrownBy(() -> walker.generate(List.of("fixtures/does-not-exist"), outputDir, toyModel()))
                .isInstanceOf(TemplateWalkException.class)
                .hasMessageContaining("fixtures/does-not-exist");
    }
}
