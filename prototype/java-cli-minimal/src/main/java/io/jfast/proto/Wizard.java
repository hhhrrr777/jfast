package io.jfast.proto;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * 自研表单向导(v2,不再用 jline-prompt —— 4.3.1 列表组件方向键渲染有 bug):
 * - 每项一屏:字段名在上,输入框/选项在下;每步清屏,不回填历史
 * - 末屏汇总所有选择,Enter 确认,数字键回改任意一项
 * - 非 TTY 直接报错,提示走全参数模式
 */
public class Wizard {

    private static final String ESC = "\u001b";
    private static final String CLEAR = ESC + "[2J" + ESC + "[H";
    private static final String HIDE_CURSOR = ESC + "[?25l";
    private static final String SHOW_CURSOR = ESC + "[?25h";
    private static final String BOLD = ESC + "[1m";
    private static final String CYAN = ESC + "[36m";
    private static final String RESET = ESC + "[0m";

    enum Type { INPUT, SELECT, CONFIRM }

    static class Field {
        final String key;
        final String label;
        final Type type;
        final String[][] options; // SELECT: {id, 说明}
        String value;

        Field(String key, String label, Type type, String value, String[][] options) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.value = value;
            this.options = options;
        }
    }

    public static Answers ask(String groupId, String artifactId, String preset, Boolean withHello)
            throws IOException {
        if (groupId != null && artifactId != null && preset != null && withHello != null) {
            return Answers.of(groupId, artifactId, preset, withHello);
        }

        List<Field> fields = new ArrayList<>();
        fields.add(new Field("groupId", "groupId", Type.INPUT,
                groupId != null ? groupId : "com.example", null));
        fields.add(new Field("artifactId", "artifactId", Type.INPUT,
                artifactId != null ? artifactId : "demo", null));
        fields.add(new Field("preset", "工程预设", Type.SELECT,
                preset != null ? preset : "empty",
                new String[][]{{"empty", "空工程 —— 仅骨架,能跑起来"},
                        {"full", "完整后台(演示) —— 骨架 + 示例模块"}}));
        fields.add(new Field("withHello", "生成 HelloController 示例", Type.CONFIRM,
                withHello != null ? withHello.toString() : "true", null));

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            if (terminal.getType().contains(Terminal.TYPE_DUMB) || System.console() == null) {
                throw new IOException("当前不是交互终端,请改用全参数模式: --group-id X --artifact-id Y --preset Z --with-hello true");
            }
            for (Field f : fields) {
                edit(terminal, f);
            }
            confirmOrEdit(terminal, fields);
        }
        return Answers.of(fields.get(0).value, fields.get(1).value,
                fields.get(2).value, Boolean.parseBoolean(fields.get(3).value));
    }

    private static void screen(Terminal terminal, String title) {
        PrintWriter w = terminal.writer();
        w.print(CLEAR);
        w.println("jfast 原型 —— 生成一个 hello 级 Spring Boot 3 工程");
        w.println();
        w.println(BOLD + title + RESET);
        w.flush();
    }

    private static void edit(Terminal terminal, Field f) throws IOException {
        switch (f.type) {
            case INPUT -> {
                screen(terminal, f.label);
                LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
                String line = reader.readLine("❯ ", null, f.value);
                if (line != null && !line.isBlank()) f.value = line.trim();
            }
            case SELECT -> {
                int sel = indexOf(f);
                Attributes saved = terminal.enterRawMode();
                PrintWriter w = terminal.writer();
                try {
                    w.print(HIDE_CURSOR);
                    while (true) {
                        screen(terminal, f.label);
                        for (int i = 0; i < f.options.length; i++) {
                            boolean cur = i == sel;
                            w.println((cur ? CYAN + "❯ " : "  ")
                                    + f.options[i][1] + (cur ? RESET : ""));
                        }
                        w.print("\n↑/↓ 选择,Enter 确认" + SHOW_CURSOR);
                        w.flush();
                        int key = readKey(terminal);
                        if (key == 'A' && sel > 0) sel--;
                        else if (key == 'B' && sel < f.options.length - 1) sel++;
                        else if (key == '\r' || key == '\n') break;
                    }
                } finally {
                    w.print(SHOW_CURSOR);
                    w.flush();
                    terminal.setAttributes(saved);
                }
                f.value = f.options[sel][0];
            }
            case CONFIRM -> {
                Attributes saved = terminal.enterRawMode();
                PrintWriter w = terminal.writer();
                try {
                    screen(terminal, f.label);
                    w.print("❯ " + ("true".equals(f.value) ? "[Y/n] " : "[y/N] "));
                    w.flush();
                    int key = readKey(terminal);
                    if (key == 'y' || key == 'Y') f.value = "true";
                    else if (key == 'n' || key == 'N') f.value = "false";
                    // Enter 保持默认值
                } finally {
                    terminal.setAttributes(saved);
                }
            }
        }
    }

    private static void confirmOrEdit(Terminal terminal, List<Field> fields) throws IOException {
        Attributes saved = terminal.enterRawMode();
        PrintWriter w = terminal.writer();
        try {
            w.print(HIDE_CURSOR);
            while (true) {
                screen(terminal, "确认你的选择");
                for (int i = 0; i < fields.size(); i++) {
                    Field f = fields.get(i);
                    w.println("  " + (i + 1) + ". " + f.label + " = " + CYAN + display(f) + RESET);
                }
                w.print("\nEnter 确认生成,数字键回改对应项" + SHOW_CURSOR);
                w.flush();
                int key = readKey(terminal);
                if (key == '\r' || key == '\n') return;
                int idx = key - '1';
                if (idx >= 0 && idx < fields.size()) {
                    terminal.setAttributes(saved);
                    edit(terminal, fields.get(idx));
                    saved = terminal.enterRawMode();
                    w.print(HIDE_CURSOR);
                }
            }
        } finally {
            w.print(SHOW_CURSOR);
            w.flush();
            terminal.setAttributes(saved);
        }
    }

    private static String display(Field f) {
        if (f.type == Type.SELECT) {
            for (String[] opt : f.options) if (opt[0].equals(f.value)) return opt[1];
        }
        if (f.type == Type.CONFIRM) return "true".equals(f.value) ? "是" : "否";
        return f.value;
    }

    private static int indexOf(Field f) {
        for (int i = 0; i < f.options.length; i++) if (f.options[i][0].equals(f.value)) return i;
        return 0;
    }

    /** 读一个键;方向键转义序列 ESC [ A/B 归一化为 'A'/'B'。 */
    private static int readKey(Terminal terminal) throws IOException {
        var reader = terminal.reader();
        int ch = reader.read();
        if (ch == 27 && reader.peek(100) == '[') {
            reader.read(); // '['
            return reader.read(); // 'A' 上 / 'B' 下 / 'C' 右 / 'D' 左
        }
        return ch;
    }
}
