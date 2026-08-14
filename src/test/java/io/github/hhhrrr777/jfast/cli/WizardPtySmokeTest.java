package io.github.hhhrrr777.jfast.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * PTY golden-path 冒烟(ADR-0009 T6):
 * 在真实伪终端下驱动两段式向导,验证「逻辑对但 UI 卡死」不发生。
 */
class WizardPtySmokeTest {

    @Test
    void selectEmptyPresetAndAcceptAllDefaults(@TempDir Path tempDir) throws Exception {
        try (PtySession session = PtySession.launch(tempDir)) {
            session.waitFor("请选择工程预设");
            session.sendEnter(); // 默认选中第一项 empty

            session.waitFor("Group ID [com.example]");
            session.sendEnter();
            session.waitFor("Artifact ID [demo]");
            session.sendEnter();
            session.waitFor("基础包名 [com.example.demo]");
            session.sendEnter();
            session.waitFor("JDK 版本:");
            session.sendEnter(); // 默认 21
            session.waitFor("数据库:");
            session.sendEnter(); // 默认 MySQL

            session.waitFor("已生成工程");
            assertThat(session.exitCode()).isEqualTo(0);

            Path readme = tempDir.resolve("demo/README.md");
            assertThat(Files.exists(readme)).isTrue();
            assertThat(Files.readString(readme)).contains("# demo").contains("数据库: mysql");
        }
    }

    @Test
    void selectDatabaseByNumber(@TempDir Path tempDir) throws Exception {
        try (PtySession session = PtySession.launch(tempDir,
                "--preset", "empty",
                "--group-id", "com.example",
                "--artifact-id", "pg-demo",
                "--jdk-version", "21")) {
            session.waitFor("基础包名 [com.example.pgdemo]"); // 连字符去除:pg-demo → pgdemo
            session.sendEnter();
            session.waitFor("数据库:");
            session.waitFor("PostgreSQL");
            session.sendLine("2"); // 选 PostgreSQL

            session.waitFor("已生成工程");
            assertThat(session.exitCode()).isEqualTo(0);

            Path readme = tempDir.resolve("pg-demo/README.md");
            assertThat(Files.readString(readme)).contains("数据库: postgresql");
        }
    }

    @Test
    void mixedModeSkipsProvidedParams(@TempDir Path tempDir) throws Exception {
        try (PtySession session = PtySession.launch(tempDir,
                "--preset", "empty",
                "--group-id", "io.github.test",
                "--artifact-id", "my-app",
                "--database", "postgresql",
                "--jdk-version", "17")) {
            // preset 已给,跳过预设屏,直接进入第二段
            session.waitFor("基础包名 [io.github.test.myapp]");
            session.sendEnter();

            session.waitFor("已生成工程");
            assertThat(session.exitCode()).isEqualTo(0);

            Path readme = tempDir.resolve("my-app/README.md");
            assertThat(Files.exists(readme)).isTrue();
            assertThat(Files.readString(readme))
                    .contains("GroupId: io.github.test")
                    .contains("数据库: postgresql");
        }
    }
}
