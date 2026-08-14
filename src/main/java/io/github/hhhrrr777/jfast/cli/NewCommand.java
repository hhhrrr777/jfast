package io.github.hhhrrr777.jfast.cli;

import io.github.hhhrrr777.jfast.core.FileTreeWalker;
import io.github.hhhrrr777.jfast.core.FreemarkerTemplateEngine;
import io.github.hhhrrr777.jfast.core.TemplateEngine;
import io.github.hhhrrr777.jfast.preset.Preset;
import io.github.hhhrrr777.jfast.preset.PresetLoader;
import io.github.hhhrrr777.jfast.wizard.Answers;
import io.github.hhhrrr777.jfast.wizard.ProjectConfiguration;
import io.github.hhhrrr777.jfast.wizard.QuestionId;
import io.github.hhhrrr777.jfast.wizard.Questionnaire;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/**
 * `jfast new` 生成工程:两段式向导 + 全参数模式双轨(ADR-0006)。
 *
 * 全参数模式(非 TTY / dumb terminal 强制):全部信息位经参数传入,向导默认值不生效,
 * 缺参一次列全并附可拷贝示例命令。
 * 混合模式(TTY 且有部分参数):已给参数跳过对应问题,缺失项走进向导补问。
 */
@Command(name = "new", mixinStandardHelpOptions = true, description = "生成新工程(按预设)。")
public final class NewCommand implements Callable<Integer> {

    private static final String BASE_LAYER = "templates/base";
    private static final String PRESETS_LAYER = "templates/presets";

    @Option(names = "--preset", description = "工程预设:empty | full(必填)。")
    String preset;

    @Option(names = "--group-id", description = "Maven groupId,如 com.example。")
    String groupId;

    @Option(names = "--artifact-id", description = "Maven artifactId,如 demo。")
    String artifactId;

    @Option(names = "--base-package", description = "Java 基础包名;缺省由 groupId + artifactId 推导。")
    String basePackage;

    @Option(names = "--jdk-version", description = "JDK 版本:17 | 21 | 25。")
    String jdkVersion;

    @Option(names = "--database", description = "数据库:mysql | postgresql | dm | kingbase | opengauss。")
    String database;

    @Option(names = "--db-host", description = "数据库主机。")
    String dbHost;

    @Option(names = "--db-port", description = "数据库端口,随 database 联动默认值。")
    String dbPort;

    @Option(names = "--db-name", description = "数据库名;缺省由 artifactId 规范化。")
    String dbName;

    @Option(names = "--db-user", description = "数据库用户名。")
    String dbUser;

    @Option(names = "--db-password", description = "数据库密码,允许为空。")
    String dbPassword;

    @Option(names = "--server-port", description = "后端服务端口。")
    String serverPort;

    @Option(names = "--output-dir", description = "输出目录;缺省 ./<artifactId>/。")
    String outputDir;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        PresetLoader presetLoader = new PresetLoader();

        if (TerminalDetector.isInteractive()) {
            return runInteractive(presetLoader);
        }
        return runFullParameterMode(presetLoader);
    }

    private Integer runFullParameterMode(PresetLoader presetLoader) throws Exception {
        if (preset == null || preset.isBlank()) {
            spec.commandLine().getErr().println("错误:非交互模式下必须提供 --preset。");
            spec.commandLine().getErr().println(exampleCommand());
            return 2;
        }

        Preset loadedPreset;
        try {
            loadedPreset = presetLoader.load(preset);
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println("错误:" + e.getMessage());
            spec.commandLine().getErr().println("可选预设:empty, full。");
            return 2;
        }

        Questionnaire questionnaire = Questionnaire.forPreset(loadedPreset);
        Answers answers = collectFromOptions();

        List<QuestionId> missing = questionnaire.missingRequired(answers);
        if (!missing.isEmpty()) {
            spec.commandLine().getErr().println("错误:缺少以下必填参数:");
            for (QuestionId id : missing) {
                spec.commandLine().getErr().println("  --" + id.paramName());
            }
            spec.commandLine().getErr().println("");
            spec.commandLine().getErr().println("可拷贝示例命令:");
            spec.commandLine().getErr().println(exampleCommand());
            return 2;
        }

        Map<QuestionId, String> errors = questionnaire.validateAll(answers);
        if (!errors.isEmpty()) {
            spec.commandLine().getErr().println("错误:以下参数校验失败:");
            for (var entry : errors.entrySet()) {
                spec.commandLine().getErr().println("  --" + entry.getKey().paramName() + ": " + entry.getValue());
            }
            return 2;
        }

        generate(questionnaire, answers);
        return 0;
    }

    private Integer runInteractive(PresetLoader presetLoader) throws Exception {
        try (TerminalWizard wizard = new TerminalWizard()) {
            String selectedPreset = preset;
            if (selectedPreset == null || selectedPreset.isBlank()) {
                List<Preset> presets = presetLoader.loadAll();
                selectedPreset = wizard.selectPreset(presets);
            }

            Preset loadedPreset;
            try {
                loadedPreset = presetLoader.load(selectedPreset);
            } catch (IllegalArgumentException e) {
                spec.commandLine().getErr().println("错误:" + e.getMessage());
                spec.commandLine().getErr().println("可选预设:empty, full。");
                return 2;
            }
            Questionnaire questionnaire = Questionnaire.forPreset(loadedPreset);
            Answers initial = collectFromOptions();

            // 命令行已给的参数直接生效并先做校验;非法则报错(与全参数模式一致)
            Map<QuestionId, String> providedErrors = questionnaire.validateAll(initial);
            if (!providedErrors.isEmpty()) {
                spec.commandLine().getErr().println("错误:以下参数校验失败:");
                for (var entry : providedErrors.entrySet()) {
                    spec.commandLine().getErr().println("  --" + entry.getKey().paramName() + ": " + entry.getValue());
                }
                return 2;
            }

            Answers answers = wizard.runQuestionnaire(questionnaire, initial);
            generate(questionnaire, answers);
            return 0;
        }
    }

    private Answers collectFromOptions() {
        Answers.Builder builder = Answers.builder();
        putIfPresent(builder, QuestionId.GROUP_ID, groupId);
        putIfPresent(builder, QuestionId.ARTIFACT_ID, artifactId);
        putIfPresent(builder, QuestionId.BASE_PACKAGE, basePackage);
        putIfPresent(builder, QuestionId.JDK_VERSION, jdkVersion);
        putIfPresent(builder, QuestionId.DATABASE, database);
        putIfPresent(builder, QuestionId.DB_HOST, dbHost);
        putIfPresent(builder, QuestionId.DB_PORT, dbPort);
        putIfPresent(builder, QuestionId.DB_NAME, dbName);
        putIfPresent(builder, QuestionId.DB_USER, dbUser);
        putIfPresent(builder, QuestionId.DB_PASSWORD, dbPassword);
        putIfPresent(builder, QuestionId.SERVER_PORT, serverPort);
        return builder.build();
    }

    private void putIfPresent(Answers.Builder builder, QuestionId id, String value) {
        // dbPassword 允许空值,故仅跳过 null(未提供),空串视为显式提供
        if (value != null) {
            builder.set(id, value);
        }
    }

    private void generate(Questionnaire questionnaire, Answers answers) {
        ProjectConfiguration config = questionnaire.toConfiguration(answers, outputDir);
        Path outputPath = Path.of(config.outputDir());

        TemplateEngine engine = new FreemarkerTemplateEngine();
        FileTreeWalker walker = new FileTreeWalker(engine);
        List<String> layers = List.of(BASE_LAYER, PRESETS_LAYER + "/" + config.preset());
        walker.generate(layers, outputPath, config.toRenderModel());

        spec.commandLine().getOut().println("已生成工程到 " + outputPath.toAbsolutePath());
        spec.commandLine().getOut().println("预设: " + config.preset());
    }

    private String exampleCommand() {
        return "jfast new --preset empty --group-id com.example --artifact-id demo "
                + "--base-package com.example.demo --jdk-version 21 --database mysql";
    }
}
