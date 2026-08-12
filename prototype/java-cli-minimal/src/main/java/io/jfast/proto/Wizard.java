package io.jfast.proto;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jline.prompt.ConfirmResult;
import org.jline.prompt.InputResult;
import org.jline.prompt.ListResult;
import org.jline.prompt.Prompter;
import org.jline.prompt.PrompterFactory;
import org.jline.prompt.PromptBuilder;
import org.jline.prompt.Prompt;
import org.jline.prompt.PromptResult;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;

/**
 * 交互向导:只追问命令行没给的项。
 * 用 JLine 4.x 新模块 jline-prompt(Inquirer.js 风格),验证交互手感。
 */
public class Wizard {

    public static Answers ask(String groupId, String artifactId, String preset, Boolean withHello)
            throws IOException {
        if (groupId != null && artifactId != null && preset != null && withHello != null) {
            return Answers.of(groupId, artifactId, preset, withHello);
        }

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            Prompter prompter = PrompterFactory.create(terminal);
            PromptBuilder builder = prompter.newBuilder();

            if (groupId == null) {
                builder.createInputPrompt()
                        .name("groupId")
                        .message("groupId?")
                        .defaultValue("com.example")
                        .addPrompt();
            }
            if (artifactId == null) {
                builder.createInputPrompt()
                        .name("artifactId")
                        .message("artifactId?")
                        .defaultValue("demo")
                        .addPrompt();
            }
            if (preset == null) {
                builder.createListPrompt()
                        .name("preset")
                        .message("选择工程预设:")
                        .newItem("empty").text("空工程 —— 仅骨架,能跑起来").add()
                        .newItem("full").text("完整后台(演示) —— 骨架 + 示例模块").add()
                        .addPrompt();
            }
            if (withHello == null) {
                builder.createConfirmPrompt()
                        .name("withHello")
                        .message("生成 HelloController 示例?")
                        .defaultValue(true)
                        .addPrompt();
            }

            List<AttributedString> header = List.of(
                    new AttributedString("jfast 原型 —— 生成一个 hello 级 Spring Boot 3 工程"));
            Map<String, ? extends PromptResult<? extends Prompt>> r =
                    prompter.prompt(header, builder.build());

            String g = groupId != null ? groupId : ((InputResult) r.get("groupId")).getInput();
            String a = artifactId != null ? artifactId : ((InputResult) r.get("artifactId")).getInput();
            String p = preset != null ? preset : ((ListResult) r.get("preset")).getSelectedId();
            boolean h = withHello != null ? withHello : ((ConfirmResult) r.get("withHello")).isConfirmed();
            return Answers.of(g, a, p, h);
        }
    }
}
