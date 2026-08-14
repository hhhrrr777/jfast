package io.github.hhhrrr777.jfast.wizard;

import io.github.hhhrrr777.jfast.preset.Preset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 问题树装配器:按预设 manifest 的 questions 白名单裁剪问题,
 * 填充默认值,逐项校验,最终生成 {@link ProjectConfiguration}。
 *
 * 本类纯逻辑,不依赖终端。
 */
public final class Questionnaire {

    private static final Set<QuestionId> OPTIONAL_QUESTIONS = Set.of(QuestionId.BASE_PACKAGE);

    private final Preset preset;
    private final List<Question> questions;

    private Questionnaire(Preset preset, List<Question> questions) {
        this.preset = preset;
        this.questions = List.copyOf(questions);
    }

    /**
     * 为指定预设装配问题树。
     *
     * @param preset 已选预设
     * @return 装配后的问题树
     */
    public static Questionnaire forPreset(Preset preset) {
        List<Question> questions = new ArrayList<>();
        for (String key : preset.questions()) {
            if ("preset".equals(key)) {
                continue; // preset 在独立单选屏已确定,不再作为表单问题
            }
            QuestionId id = QuestionId.fromKey(key);
            questions.add(buildQuestion(id));
        }
        return new Questionnaire(preset, questions);
    }

    public Preset preset() {
        return preset;
    }

    public List<Question> questions() {
        return questions;
    }

    /**
     * 用默认值补齐缺失答案,保持问题顺序。
     *
     * 动态默认值可能依赖前置答案,因此按问题列表顺序逐项填充。
     */
    public Answers applyDefaults(Answers partial) {
        Answers result = partial;
        for (Question question : questions) {
            if (!result.contains(question.id())) {
                result = result.with(question.id(), question.defaultFor(result));
            }
        }
        return result;
    }

    /**
     * 校验全部已提供的答案,返回首个失败的校验信息(按问题顺序)。
     *
     * 缺失的答案不校验(缺失性由 {@link #missingRequired} 单独检查);
     * 可选问题未提供时跳过,由其默认值推导兜底。
     *
     * @return 若全部通过返回空 Optional
     */
    public Optional<Map.Entry<QuestionId, String>> validateFirst(Answers answers) {
        for (Question question : questions) {
            if (answers.get(question.id()).isEmpty()) {
                continue;
            }
            String value = answers.get(question.id()).orElse("");
            ValidationResult result = question.validate(value);
            if (!result.valid()) {
                return Optional.of(Map.entry(question.id(), result.errorMessage()));
            }
        }
        return Optional.empty();
    }

    /**
     * 校验全部已提供的答案,返回所有错误。
     */
    public Map<QuestionId, String> validateAll(Answers answers) {
        Map<QuestionId, String> errors = new LinkedHashMap<>();
        for (Question question : questions) {
            if (answers.get(question.id()).isEmpty()) {
                continue;
            }
            String value = answers.get(question.id()).orElse("");
            ValidationResult result = question.validate(value);
            if (!result.valid()) {
                errors.put(question.id(), result.errorMessage());
            }
        }
        return errors;
    }

    /**
     * 检查全参数模式下缺失的必填项(不含可推导的 basePackage/outputDir)。
     */
    public List<QuestionId> missingRequired(Answers answers) {
        List<QuestionId> missing = new ArrayList<>();
        for (Question question : questions) {
            if (OPTIONAL_QUESTIONS.contains(question.id())) {
                continue;
            }
            if (answers.get(question.id()).isEmpty()) {
                missing.add(question.id());
            }
        }
        return missing;
    }

    /**
     * 根据完整答案生成工程配置。
     *
     * @param answers   完整答案(可含 preset)
     * @param outputDir 输出目录,为 null 时按 artifactId 推导
     */
    public ProjectConfiguration toConfiguration(Answers answers, String outputDir) {
        Answers filled = applyDefaults(answers);

        String groupId = filled.get(QuestionId.GROUP_ID).orElseThrow();
        String artifactId = filled.get(QuestionId.ARTIFACT_ID).orElseThrow();
        String basePackage = filled.get(QuestionId.BASE_PACKAGE).orElse("");
        if (basePackage.isBlank()) {
            basePackage = WizardDefaults.deriveBasePackage(groupId, artifactId);
        }
        String packagePath = basePackage.replace('.', '/');
        String projectName = artifactId;
        String description = artifactId + " 工程";
        String jdkVersion = filled.get(QuestionId.JDK_VERSION).orElse("21");
        String database = filled.get(QuestionId.DATABASE).orElse("mysql");
        String dbHost = filled.getOrDefault(QuestionId.DB_HOST, "localhost");
        String dbPort = filled.get(QuestionId.DB_PORT).orElseGet(() -> WizardDefaults.defaultDbPort(database));
        String dbName = filled.get(QuestionId.DB_NAME).orElseGet(() -> WizardDefaults.deriveDbName(artifactId));
        String dbUser = filled.getOrDefault(QuestionId.DB_USER, "root");
        String dbPassword = filled.get(QuestionId.DB_PASSWORD).orElse("");
        String serverPort = filled.getOrDefault(QuestionId.SERVER_PORT, "8080");
        String finalOutputDir = outputDir != null ? outputDir : "./" + artifactId + "/";

        return new ProjectConfiguration(
                preset.name(),
                groupId,
                artifactId,
                basePackage,
                packagePath,
                projectName,
                description,
                jdkVersion,
                database,
                dbHost,
                dbPort,
                dbName,
                dbUser,
                dbPassword,
                serverPort,
                finalOutputDir,
                Map.copyOf(preset.conditions()));
    }

    private static Question buildQuestion(QuestionId id) {
        return switch (id) {
            case GROUP_ID -> new Question(
                    QuestionId.GROUP_ID,
                    "Group ID",
                    "工程坐标",
                    a -> "com.example",
                    List.of(),
                    WizardDefaults::validatePackageName);
            case ARTIFACT_ID -> new Question(
                    QuestionId.ARTIFACT_ID,
                    "Artifact ID",
                    "工程坐标",
                    a -> "demo",
                    List.of(),
                    WizardDefaults::validateArtifactId);
            case BASE_PACKAGE -> new Question(
                    QuestionId.BASE_PACKAGE,
                    "基础包名",
                    "工程坐标",
                    a -> WizardDefaults.deriveBasePackage(
                            a.getOrDefault(QuestionId.GROUP_ID, "com.example"),
                            a.getOrDefault(QuestionId.ARTIFACT_ID, "demo")),
                    List.of(),
                    WizardDefaults::validatePackageName);
            case JDK_VERSION -> new Question(
                    QuestionId.JDK_VERSION,
                    "JDK 版本",
                    "工程坐标",
                    a -> "21",
                    WizardDefaults.jdkVersions().stream().map(v -> new Choice(v, v)).toList(),
                    WizardDefaults::validateJdkVersion);
            case DATABASE -> new Question(
                    QuestionId.DATABASE,
                    "数据库",
                    "数据库",
                    a -> "mysql",
                    WizardDefaults.databases(),
                    WizardDefaults::validateDatabase);
            case DB_HOST -> new Question(
                    QuestionId.DB_HOST,
                    "数据库主机",
                    "数据库",
                    a -> "localhost",
                    List.of(),
                    WizardDefaults::validateNonEmpty);
            case DB_PORT -> new Question(
                    QuestionId.DB_PORT,
                    "数据库端口",
                    "数据库",
                    a -> WizardDefaults.defaultDbPort(a.getOrDefault(QuestionId.DATABASE, "mysql")),
                    List.of(),
                    WizardDefaults::validatePort);
            case DB_NAME -> new Question(
                    QuestionId.DB_NAME,
                    "数据库名",
                    "数据库",
                    a -> WizardDefaults.deriveDbName(a.getOrDefault(QuestionId.ARTIFACT_ID, "demo")),
                    List.of(),
                    WizardDefaults::validateNonEmpty);
            case DB_USER -> new Question(
                    QuestionId.DB_USER,
                    "数据库用户名",
                    "数据库",
                    a -> "root",
                    List.of(),
                    WizardDefaults::validateNonEmpty);
            case DB_PASSWORD -> new Question(
                    QuestionId.DB_PASSWORD,
                    "数据库密码",
                    "数据库",
                    a -> "password123",
                    List.of(),
                    value -> ValidationResult.ok()); // 允许空
            case SERVER_PORT -> new Question(
                    QuestionId.SERVER_PORT,
                    "服务端口",
                    "运行",
                    a -> "8080",
                    List.of(),
                    WizardDefaults::validatePort);
            default -> throw new IllegalStateException("未定义的问题: " + id);
        };
    }
}
