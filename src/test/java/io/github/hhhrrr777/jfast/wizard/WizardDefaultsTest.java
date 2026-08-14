package io.github.hhhrrr777.jfast.wizard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class WizardDefaultsTest {

    @Test
    void deriveBasePackageConcatenatesAndStripsHyphens() {
        assertThat(WizardDefaults.deriveBasePackage("com.example", "my-app"))
                .isEqualTo("com.example.myapp");
    }

    @Test
    void deriveBasePackageFallsBackWhenGroupIdMissing() {
        assertThat(WizardDefaults.deriveBasePackage(null, "demo"))
                .isEqualTo("com.example.demo");
    }

    @Test
    void deriveDbNameReplacesHyphensWithUnderscore() {
        assertThat(WizardDefaults.deriveDbName("my-app")).isEqualTo("my_app");
        assertThat(WizardDefaults.deriveDbName("MyApp")).isEqualTo("myapp");
    }

    @ParameterizedTest
    @CsvSource({
            "mysql, 3306",
            "postgresql, 5432",
            "dm, 5236",
            "kingbase, 54321",
            "opengauss, 5432"
    })
    void defaultDbPortByDatabase(String database, String expectedPort) {
        assertThat(WizardDefaults.defaultDbPort(database)).isEqualTo(expectedPort);
    }

    @ParameterizedTest
    @ValueSource(strings = {"com.example", "io.github.hhhrrr777", "a.b.c.d"})
    void validPackageName(String value) {
        assertThat(WizardDefaults.validatePackageName(value).valid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"com", "com.1example", "com.my class", "com.abstract", ""})
    void invalidPackageName(String value) {
        assertThat(WizardDefaults.validatePackageName(value).valid()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"demo", "my-app", "app2"})
    void validArtifactId(String value) {
        assertThat(WizardDefaults.validateArtifactId(value).valid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MyApp", "2app", "my_app", ""})
    void invalidArtifactId(String value) {
        assertThat(WizardDefaults.validateArtifactId(value).valid()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1024", "3306", "65535"})
    void validPort(String value) {
        assertThat(WizardDefaults.validatePort(value).valid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"80", "65536", "abc", ""})
    void invalidPort(String value) {
        assertThat(WizardDefaults.validatePort(value).valid()).isFalse();
    }
}
