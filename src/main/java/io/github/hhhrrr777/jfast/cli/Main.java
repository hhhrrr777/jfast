package io.github.hhhrrr777.jfast.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/** jfast CLI 入口:裸 `jfast` 打印 help。 */
@Command(name = "jfast",
        mixinStandardHelpOptions = true,
        version = "jfast 0.1.0-SNAPSHOT",
        description = "jfast 工程生成器:脚手架生成 + 实体建模生成。",
        subcommands = {NewCommand.class, EntityCommand.class})
public final class Main implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
