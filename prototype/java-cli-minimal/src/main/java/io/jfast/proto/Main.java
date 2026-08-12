package io.jfast.proto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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

    @Option(names = "--force", description = "目标目录已存在且非空时,先删除再生成")
    boolean force;

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public Integer call() {
        try {
            Answers answers = Wizard.ask(groupId, artifactId, preset, withHello);
            Path target = output.toAbsolutePath().resolve(answers.artifactId());
            prepareTarget(target);
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

    /**
     * 目标目录守卫:覆盖生成会在旧包路径下残留过期类(Spring Boot 会因多个主类拒跑),
     * 所以非空目录一律拒绝,除非 --force 显式清目录。
     */
    private void prepareTarget(Path target) throws IOException {
        if (!Files.isDirectory(target)) return;
        boolean empty;
        try (var s = Files.list(target)) {
            empty = s.findAny().isEmpty();
        }
        if (empty) return;
        if (!force) {
            throw new IOException("目标目录已存在且非空: " + target + " —— 换个 artifact-id,或加 --force 清目录重新生成");
        }
        try (var s = Files.walk(target)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }
}
