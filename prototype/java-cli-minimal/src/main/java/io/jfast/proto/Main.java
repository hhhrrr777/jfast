package io.jfast.proto;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * jfast 生成器最小原型入口。
 * 所有选项缺失时进入交互向导;全部给出时纯非交互(便于脚本/CI 验证)。
 */
@Command(name = "jfast-proto", mixinStandardHelpOptions = true,
        description = "jfast 生成器最小原型(PROTOTYPE,扔掉的验证物)")
public class Main implements Callable<Integer> {

    @Option(names = "--group-id", description = "目标工程 groupId")
    String groupId;

    @Option(names = "--artifact-id", description = "目标工程 artifactId")
    String artifactId;

    @Option(names = "--preset", description = "预设: empty(空工程) | full(完整后台演示)")
    String preset;

    @Option(names = "--with-hello", arity = "1", description = "是否生成 HelloController 示例: true | false")
    Boolean withHello;

    @Option(names = {"-o", "--output"}, defaultValue = ".", description = "输出目录(默认当前目录)")
    Path output;

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public Integer call() {
        try {
            Answers answers = Wizard.ask(groupId, artifactId, preset, withHello);
            Path target = output.toAbsolutePath().resolve(answers.artifactId());
            new Generator().generate(answers, target);
            System.out.println();
            System.out.println("✔ 已生成: " + target);
            System.out.println("  下一步: cd " + answers.artifactId() + " && mvn spring-boot:run");
            return 0;
        } catch (Exception e) {
            System.err.println("✘ 生成失败: " + e.getMessage());
            return 1;
        }
    }
}
