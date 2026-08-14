package io.github.hhhrrr777.jfast.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/** `jfast entity` 空壳(实体建模归 S4 阶段施工单元)。 */
@Command(name = "entity", mixinStandardHelpOptions = true, description = "实体建模:在目标工程内生成实体与 CRUD 代码。")
public final class EntityCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().getOut().println("jfast entity 尚未实现:实体建模归施工单元 S4-1 起。");
        return 0;
    }
}
