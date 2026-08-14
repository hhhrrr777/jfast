package io.github.hhhrrr777.jfast.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/** `jfast new` 空壳(交互/参数模式归 S1-2,端到端生成归 S1-5)。 */
@Command(name = "new", mixinStandardHelpOptions = true, description = "生成新工程(按预设)。")
public final class NewCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().getOut().println("jfast new 尚未实现:交互/参数模式归施工单元 S1-2,端到端生成归 S1-5。");
        return 0;
    }
}
