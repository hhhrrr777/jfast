package io.github.hhhrrr777.jfast.preset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PresetLoaderTest {

    private final PresetLoader loader = new PresetLoader();

    @Test
    void loadsEmptyPreset() {
        Preset preset = loader.load("empty");

        assertThat(preset.name()).isEqualTo("empty");
        assertThat(preset.displayName()).isEqualTo("空工程");
        assertThat(preset.questions()).containsExactly(
                "preset", "groupId", "artifactId", "basePackage", "jdkVersion", "database");
        assertThat(preset.conditions()).containsEntry("systemAdmin", false);
    }

    @Test
    void loadsFullPreset() {
        Preset preset = loader.load("full");

        assertThat(preset.name()).isEqualTo("full");
        assertThat(preset.displayName()).isEqualTo("完整后台");
        assertThat(preset.questions()).contains(
                "dbHost", "dbPort", "dbName", "dbUser", "dbPassword", "serverPort");
        assertThat(preset.conditions()).containsEntry("systemAdmin", true);
    }

    @Test
    void loadsAllPresetsSorted() {
        var presets = loader.loadAll();

        assertThat(presets).hasSize(2);
        assertThat(presets.get(0).name()).isEqualTo("empty");
        assertThat(presets.get(1).name()).isEqualTo("full");
    }

    @Test
    void rejectsUnknownPreset() {
        assertThatThrownBy(() -> loader.load("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("预设不存在");
    }
}
