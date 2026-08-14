package io.github.hhhrrr777.jfast.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class MainTest {

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
    void bareJfastPrintsHelp() {
        Run result = run();

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.out()).contains("jfast").contains("new").contains("entity");
    }

    @Test
    void newCommandShellRuns() {
        Run result = run("new");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.out()).contains("尚未实现");
    }

    @Test
    void entityCommandShellRuns() {
        Run result = run("entity");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.out()).contains("尚未实现");
    }
}
