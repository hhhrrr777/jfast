package io.github.hhhrrr777.jfast.wizard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hhhrrr777.jfast.preset.Preset;
import io.github.hhhrrr777.jfast.preset.PresetLoader;
import org.junit.jupiter.api.Test;

class QuestionnaireTest {

    private final PresetLoader loader = new PresetLoader();

    @Test
    void emptyPresetAssemblesCoordinateAndDatabaseQuestions() {
        Preset preset = loader.load("empty");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);

        assertThat(questionnaire.questions())
                .extracting(Question::id)
                .containsExactly(
                        QuestionId.GROUP_ID,
                        QuestionId.ARTIFACT_ID,
                        QuestionId.BASE_PACKAGE,
                        QuestionId.JDK_VERSION,
                        QuestionId.DATABASE);
    }

    @Test
    void fullPresetAssemblesAllQuestions() {
        Preset preset = loader.load("full");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);

        assertThat(questionnaire.questions())
                .extracting(Question::id)
                .containsExactly(
                        QuestionId.GROUP_ID,
                        QuestionId.ARTIFACT_ID,
                        QuestionId.BASE_PACKAGE,
                        QuestionId.JDK_VERSION,
                        QuestionId.DATABASE,
                        QuestionId.DB_HOST,
                        QuestionId.DB_PORT,
                        QuestionId.DB_NAME,
                        QuestionId.DB_USER,
                        QuestionId.DB_PASSWORD,
                        QuestionId.SERVER_PORT);
    }

    @Test
    void applyDefaultsFillsAllFields() {
        Preset preset = loader.load("empty");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);
        Answers answers = questionnaire.applyDefaults(Answers.empty());

        assertThat(answers.get(QuestionId.GROUP_ID)).hasValue("com.example");
        assertThat(answers.get(QuestionId.ARTIFACT_ID)).hasValue("demo");
        assertThat(answers.get(QuestionId.BASE_PACKAGE)).hasValue("com.example.demo");
        assertThat(answers.get(QuestionId.JDK_VERSION)).hasValue("21");
        assertThat(answers.get(QuestionId.DATABASE)).hasValue("mysql");
    }

    @Test
    void basePackageDerivesFromGivenAnswers() {
        Preset preset = loader.load("empty");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);
        Answers partial = Answers.builder()
                .set(QuestionId.GROUP_ID, "io.github.test")
                .set(QuestionId.ARTIFACT_ID, "my-app")
                .build();

        Answers answers = questionnaire.applyDefaults(partial);

        assertThat(answers.get(QuestionId.BASE_PACKAGE)).hasValue("io.github.test.myapp");
    }

    @Test
    void dbPortFollowsDatabaseDefault() {
        Preset preset = loader.load("full");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);
        Answers answers = questionnaire.applyDefaults(Answers.empty());

        assertThat(answers.get(QuestionId.DB_PORT)).hasValue("3306");
    }

    @Test
    void validateAllReportsErrors() {
        Preset preset = loader.load("empty");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);
        Answers answers = Answers.builder()
                .set(QuestionId.GROUP_ID, "com")
                .set(QuestionId.ARTIFACT_ID, "MyApp")
                .set(QuestionId.JDK_VERSION, "11")
                .set(QuestionId.DATABASE, "oracle")
                .build();

        var errors = questionnaire.validateAll(answers);

        assertThat(errors).containsKeys(
                QuestionId.GROUP_ID,
                QuestionId.ARTIFACT_ID,
                QuestionId.JDK_VERSION,
                QuestionId.DATABASE);
    }

    @Test
    void toConfigurationProducesRenderModel() {
        Preset preset = loader.load("full");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);
        Answers answers = Answers.builder()
                .set(QuestionId.GROUP_ID, "io.github.hhhrrr777")
                .set(QuestionId.ARTIFACT_ID, "jfast-demo")
                .set(QuestionId.JDK_VERSION, "21")
                .set(QuestionId.DATABASE, "postgresql")
                .set(QuestionId.DB_HOST, "127.0.0.1")
                .set(QuestionId.DB_PORT, "5432")
                .set(QuestionId.DB_NAME, "jfast_demo")
                .set(QuestionId.DB_USER, "postgres")
                .set(QuestionId.DB_PASSWORD, "secret")
                .set(QuestionId.SERVER_PORT, "9090")
                .build();

        ProjectConfiguration config = questionnaire.toConfiguration(answers, null);

        assertThat(config.preset()).isEqualTo("full");
        assertThat(config.groupId()).isEqualTo("io.github.hhhrrr777");
        assertThat(config.artifactId()).isEqualTo("jfast-demo");
        assertThat(config.basePackage()).isEqualTo("io.github.hhhrrr777.jfastdemo");
        assertThat(config.packagePath()).isEqualTo("io/github/hhhrrr777/jfastdemo");
        assertThat(config.database()).isEqualTo("postgresql");
        assertThat(config.dbPort()).isEqualTo("5432");
        assertThat(config.serverPort()).isEqualTo("9090");
        assertThat(config.outputDir()).isEqualTo("./jfast-demo/");
        assertThat(config.conditions()).containsEntry("systemAdmin", true);
    }

    @Test
    void emptyPresetConditionIsFalse() {
        Preset preset = loader.load("empty");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);
        ProjectConfiguration config = questionnaire.toConfiguration(Answers.empty(), null);

        assertThat(config.conditions()).containsEntry("systemAdmin", false);
    }

    @Test
    void validateAllSkipsAbsentOptionalBasePackage() {
        Preset preset = loader.load("empty");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);
        Answers answers = Answers.builder()
                .set(QuestionId.GROUP_ID, "com.example")
                .set(QuestionId.ARTIFACT_ID, "demo")
                .set(QuestionId.JDK_VERSION, "21")
                .set(QuestionId.DATABASE, "mysql")
                .build();

        // basePackage 缺省由 groupId + artifactId 推导,不应被当作错误
        assertThat(questionnaire.validateAll(answers)).isEmpty();
    }

    @Test
    void paramNamesAreKebabCase() {
        assertThat(QuestionId.GROUP_ID.paramName()).isEqualTo("group-id");
        assertThat(QuestionId.BASE_PACKAGE.paramName()).isEqualTo("base-package");
        assertThat(QuestionId.DB_PASSWORD.paramName()).isEqualTo("db-password");
        assertThat(QuestionId.SERVER_PORT.paramName()).isEqualTo("server-port");
    }

    @Test
    void missingRequiredExcludesOptionalBasePackage() {
        Preset preset = loader.load("empty");
        Questionnaire questionnaire = Questionnaire.forPreset(preset);

        assertThat(questionnaire.missingRequired(Answers.empty()))
                .containsExactly(
                        QuestionId.GROUP_ID,
                        QuestionId.ARTIFACT_ID,
                        QuestionId.JDK_VERSION,
                        QuestionId.DATABASE);
    }
}
