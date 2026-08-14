package io.github.hhhrrr777.jfast.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class NewCommandTest {

    private record Run(int exitCode, String out, String err) {}

    private static Run run(String... args) {
        CommandLine cmd = new CommandLine(new Main());
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));
        int exitCode = cmd.execute(args);
        return new Run(exitCode, out.toString(), err.toString());
    }

    @Test
    void missingPresetInFullParameterModeErrorsWithExample() {
        Run result = run("new");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("--preset");
        assertThat(result.err()).contains("jfast new --preset empty");
    }

    @Test
    void missingRequiredParamsListedAtOnce() {
        Run result = run("new", "--preset", "full");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err())
                .contains("--group-id")
                .contains("--artifact-id")
                .contains("--jdk-version")
                .contains("--database")
                .contains("--db-host")
                .contains("--db-user")
                .contains("--db-password")
                .contains("--server-port")
                .contains("可拷贝示例命令");
    }

    @Test
    void invalidParamsReported() {
        Run result = run("new", "--preset", "empty",
                "--group-id", "com", "--artifact-id", "MyApp",
                "--jdk-version", "11", "--database", "oracle");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("校验失败");
    }

    @Test
    void unknownPresetRejected() {
        Run result = run("new", "--preset", "nope");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.err()).contains("预设不存在");
        assertThat(result.err()).contains("empty, full");
    }

    @Test
    void fullPresetAllowsEmptyDbPassword(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("gen");
        Run result = run("new",
                "--preset", "full",
                "--group-id", "com.example",
                "--artifact-id", "demo",
                "--jdk-version", "21",
                "--database", "mysql",
                "--db-host", "localhost",
                "--db-port", "3306",
                "--db-name", "demo",
                "--db-user", "root",
                "--db-password", "",
                "--server-port", "8080",
                "--output-dir", output.toString());

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(Files.exists(output.resolve("README.md"))).isTrue();
    }

    @Test
    void emptyPresetFullParameterGeneratesProject(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("gen");
        Run result = run("new",
                "--preset", "empty",
                "--group-id", "com.example",
                "--artifact-id", "demo",
                "--jdk-version", "21",
                "--database", "mysql",
                "--output-dir", output.toString());

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.out()).contains("已生成工程");

        Path readme = output.resolve("README.md");
        assertThat(Files.exists(readme)).isTrue();
        String content = Files.readString(readme);
        assertThat(content).contains("# demo");
        assertThat(content).contains("GroupId: com.example");
        assertThat(content).contains("数据库: mysql");
    }

    @Test
    void fullPresetRendersConditionalBlock(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("gen");
        Run result = run("new",
                "--preset", "full",
                "--group-id", "com.example",
                "--artifact-id", "admin-app",
                "--jdk-version", "21",
                "--database", "postgresql",
                "--db-host", "127.0.0.1",
                "--db-port", "5432",
                "--db-name", "admin_app",
                "--db-user", "postgres",
                "--db-password", "secret",
                "--server-port", "9090",
                "--output-dir", output.toString());

        assertThat(result.exitCode()).isEqualTo(0);

        String content = Files.readString(output.resolve("README.md"));
        assertThat(content).contains("数据库主机: 127.0.0.1:5432");
        assertThat(content).contains("服务端口: 9090");
    }
}
