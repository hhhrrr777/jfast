package io.github.hhhrrr777.jfast.cli;

import io.github.hhhrrr777.jfast.preset.Preset;
import io.github.hhhrrr777.jfast.wizard.Answers;
import io.github.hhhrrr777.jfast.wizard.Choice;
import io.github.hhhrrr777.jfast.wizard.Question;
import io.github.hhhrrr777.jfast.wizard.QuestionId;
import io.github.hhhrrr777.jfast.wizard.Questionnaire;
import io.github.hhhrrr777.jfast.wizard.ValidationResult;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * 两段式向导交互层(ADR-0006)。
 *
 * 第一段:预设独立单选屏(必选无默认)。
 * 第二段:按 Tab 骨架(工程坐标/数据库/运行)顺序的表单,字段按预设白名单增减。
 *
 * 实现基于 jline-terminal 裸原语:显式清屏、光标控制、按键读取。
 */
public final class TerminalWizard implements AutoCloseable {

    private final Terminal terminal;
    private final PrintWriter out;

    public TerminalWizard() throws IOException {
        this(TerminalBuilder.terminal());
    }

    public TerminalWizard(Terminal terminal) {
        this.terminal = terminal;
        this.out = terminal.writer();
    }

    /** 运行预设单选屏,返回选中的预设名。 */
    public String selectPreset(List<Preset> presets) throws IOException {
        if (presets.isEmpty()) {
            throw new IllegalStateException("没有可用预设");
        }
        int selected = 0;
        while (true) {
            clearScreen();
            println("请选择工程预设:");
            println("");
            for (int i = 0; i < presets.size(); i++) {
                String marker = i == selected ? "> " : "  ";
                Preset preset = presets.get(i);
                println(marker + preset.displayName() + " - " + preset.description());
            }
            println("");
            println("使用 ↑/↓ 选择,Enter 确认");
            out.flush();

            int key = readKey();
            if (key == '') {
                int next = terminal.reader().read();
                if (next == '[') {
                    int arrow = terminal.reader().read();
                    if (arrow == 'A') { // up
                        selected = (selected - 1 + presets.size()) % presets.size();
                    } else if (arrow == 'B') { // down
                        selected = (selected + 1) % presets.size();
                    }
                }
            } else if (key == '\r' || key == '\n') {
                return presets.get(selected).name();
            }
        }
    }

    /**
     * 运行第二段 Tab 表单,用已给参数跳过对应问题。
     *
     * @param questionnaire 装配好的问题树
     * @param initial       已从命令行传入的答案
     * @return 完整答案
     */
    public Answers runQuestionnaire(Questionnaire questionnaire, Answers initial) throws IOException {
        Answers answers = initial;
        List<Question> questions = questionnaire.questions();
        List<String> tabs = questions.stream().map(Question::tab).distinct().toList();

        for (int tabIndex = 0; tabIndex < tabs.size(); tabIndex++) {
            String tab = tabs.get(tabIndex);
            List<Question> tabQuestions = questions.stream()
                    .filter(q -> q.tab().equals(tab))
                    .toList();

            for (Question question : tabQuestions) {
                if (answers.contains(question.id())) {
                    // 命令行已提供,只做校验;失败则进入交互重输
                    ValidationResult result = question.validate(answers.get(question.id()).orElse(""));
                    if (result.valid()) {
                        continue;
                    }
                }
                answers = askOne(answers, question, tab, tabIndex + 1, tabs.size());
            }
        }
        return answers;
    }

    private Answers askOne(Answers answers, Question question, String tab, int tabNumber, int totalTabs)
            throws IOException {
        while (true) {
            clearScreen();
            println("Tab " + tabNumber + "/" + totalTabs + ": " + tab);
            println("");
            String current = answers.contains(question.id())
                    ? answers.get(question.id()).orElse("")
                    : question.defaultFor(answers);

            if (question.isChoice()) {
                return askChoice(answers, question, current);
            }

            println(question.displayName() + " [" + current + "]: ");
            out.flush();

            String input = readLine().trim();
            String value = input.isEmpty() ? current : input;

            ValidationResult result = question.validate(value);
            if (!result.valid()) {
                println(ansiRed("✗ " + result.errorMessage()));
                println("按 Enter 重新输入...");
                out.flush();
                waitEnter();
                continue;
            }
            return answers.with(question.id(), value);
        }
    }

    /** 选择题(如数据库/JDK 版本):列出带中文显示名的选项,支持数字/值/默认回车。 */
    private Answers askChoice(Answers answers, Question question, String current) throws IOException {
        List<Choice> choices = question.choices();
        int currentIndex = indexOfChoice(choices, current);
        println(question.displayName() + ":");
        for (int i = 0; i < choices.size(); i++) {
            Choice choice = choices.get(i);
            String marker = i == currentIndex ? "> " : "  ";
            println(marker + (i + 1) + ". " + choice.displayName());
        }
        println("");
        println("输入数字选择或 Enter 确认(" + choices.get(Math.max(currentIndex, 0)).displayName() + ")");
        out.flush();

        while (true) {
            String input = readLine().trim();
            if (input.isEmpty()) {
                return answers.with(question.id(), choices.get(Math.max(currentIndex, 0)).value());
            }
            try {
                int n = Integer.parseInt(input);
                if (n >= 1 && n <= choices.size()) {
                    return answers.with(question.id(), choices.get(n - 1).value());
                }
            } catch (NumberFormatException ignored) {
                // 尝试按值或显示名匹配
            }
            for (Choice choice : choices) {
                if (choice.value().equalsIgnoreCase(input) || choice.displayName().equalsIgnoreCase(input)) {
                    return answers.with(question.id(), choice.value());
                }
            }
            println("无效选择,请输入 1-" + choices.size() + " 或 Enter 确认");
            out.flush();
        }
    }

    private int indexOfChoice(List<Choice> choices, String value) {
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).value().equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int key = terminal.reader().read();
            if (key == -1 || key == '\r' || key == '\n') {
                break;
            }
            if (key == '\b' || key == 127) { // backspace
                if (!sb.isEmpty()) {
                    sb.deleteCharAt(sb.length() - 1);
                    out.print("\b \b");
                    out.flush();
                }
                continue;
            }
            if (key >= 32 && key < 127) {
                sb.append((char) key);
                out.print((char) key);
                out.flush();
            }
        }
        out.println();
        return sb.toString();
    }

    private void waitEnter() throws IOException {
        while (true) {
            int key = terminal.reader().read();
            if (key == '\r' || key == '\n' || key == -1) {
                break;
            }
        }
    }

    private int readKey() throws IOException {
        return terminal.reader().read();
    }

    private void clearScreen() {
        out.print("\033[H\033[2J");
        out.flush();
    }

    private void println(String line) {
        out.println(line);
    }

    private String ansiRed(String text) {
        return "\033[31m" + text + "\033[0m";
    }

    @Override
    public void close() throws IOException {
        terminal.close();
    }
}
