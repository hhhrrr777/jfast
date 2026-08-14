package io.github.hhhrrr777.jfast.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hhhrrr777.jfast.preset.PresetLoader;
import io.github.hhhrrr777.jfast.wizard.Answers;
import io.github.hhhrrr777.jfast.wizard.QuestionId;
import io.github.hhhrrr777.jfast.wizard.Questionnaire;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * S1-3 base 后端骨架模板:五种数据库各渲染一次,断言关键文件与内容分叉
 * (驱动坐标、URL 模板、端口联动、MyBatis-Plus 方言)。实际 mvn compile 门禁归 S1-5 T2。
 *
 * 渲染模型走真实的 Questionnaire.toConfiguration(...).toRenderModel()(S1-2 契约),
 * 不手工伪造,确保模板引用的键与模型生产方严格一致。
 */
class BackendSkeletonTemplateTest {

    private final FileTreeWalker walker = new FileTreeWalker(new FreemarkerTemplateEngine());
    private final PresetLoader presetLoader = new PresetLoader();

    private static Stream<Arguments> databases() {
        return Stream.of(
                Arguments.of("mysql", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://", "3306", "MYSQL"),
                Arguments.of("postgresql", "org.postgresql.Driver", "jdbc:postgresql://", "5432", "POSTGRE_SQL"),
                Arguments.of("dm", "dm.jdbc.driver.DmDriver", "jdbc:dm://", "5236", "DM"),
                Arguments.of("kingbase", "com.kingbase8.Driver", "jdbc:kingbase8://", "54321", "KINGBASE_ES"),
                Arguments.of("opengauss", "org.opengauss.Driver", "jdbc:opengauss://", "5432", "POSTGRE_SQL"));
    }

    @ParameterizedTest
    @MethodSource("databases")
    void rendersBackendSkeleton(String database, String driverClass, String urlPrefix,
                                String defaultPort, String dbType, @TempDir Path outputDir) throws Exception {
        walker.generate(List.of("templates/base"), outputDir, model(database, defaultPort));

        // 包路径单占位目录段 ${project.packagePath}:渲染成斜杠分隔路径
        Path mainDir = outputDir.resolve("src/main/java/com/example/demo");
        assertThat(mainDir).isDirectory();
        assertThat(mainDir.resolve("DemoApplication.java")).content().startsWith("package com.example.demo;");

        // 无后缀文件字节拷贝
        assertThat(outputDir.resolve(".gitignore")).exists();

        // pom.xml:坐标 + 版本定版
        String pom = Files.readString(outputDir.resolve("pom.xml"));
        assertThat(pom).contains("<groupId>com.example</groupId>");
        assertThat(pom).contains("<artifactId>demo</artifactId>");
        assertThat(pom).contains("<version>3.5.16</version>");      // Spring Boot parent
        assertThat(pom).contains("<mybatis-plus.version>3.5.17</mybatis-plus.version>");
        assertThat(pom).contains("<jjwt.version>0.13.0</jjwt.version>");
        assertThat(pom).contains("mybatis-plus-jsqlparser");

        // application.yml:驱动 + URL + 端口联动默认值
        String yml = Files.readString(outputDir.resolve("src/main/resources/application.yml"));
        assertThat(yml).contains("driver-class-name: " + driverClass);
        assertThat(yml).contains("url: " + urlPrefix + "localhost:" + defaultPort + "/demo");
        assertThat(yml).contains("port: 8080");

        // MyBatis-Plus 分页方言分叉
        String config = Files.readString(mainDir.resolve("config/MyBatisPlusConfig.java"));
        assertThat(config).contains("DbType." + dbType);

        // 测试类与主类同形
        assertThat(outputDir.resolve("src/test/java/com/example/demo/DemoApplicationTests.java"))
                .content().contains("class DemoApplicationTests");
    }

    /** 经 S1-2 契约(Questionnaire → ProjectConfiguration.toRenderModel)构造真实渲染模型。 */
    private Map<String, Object> model(String database, String defaultPort) {
        Questionnaire questionnaire = Questionnaire.forPreset(presetLoader.load("full"));
        Answers answers = Answers.builder()
                .set(QuestionId.GROUP_ID, "com.example")
                .set(QuestionId.ARTIFACT_ID, "demo")
                .set(QuestionId.BASE_PACKAGE, "com.example.demo")
                .set(QuestionId.JDK_VERSION, "21")
                .set(QuestionId.DATABASE, database)
                .set(QuestionId.DB_HOST, "localhost")
                .set(QuestionId.DB_PORT, defaultPort)
                .set(QuestionId.DB_NAME, "demo")
                .set(QuestionId.DB_USER, "root")
                .set(QuestionId.DB_PASSWORD, "password123")
                .set(QuestionId.SERVER_PORT, "8080")
                .build();
        return questionnaire.toConfiguration(answers, "unused").toRenderModel();
    }
}
