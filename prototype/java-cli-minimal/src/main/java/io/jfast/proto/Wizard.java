package io.jfast.proto;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * 自研表单向导 v3:单屏 Tab 表单。
 * - 顶部标题 + Tab 栏(GroupId / ArtifactId / 工程预设 / HelloController 示例 / 确认)
 * - 活动 Tab 蓝色(#2B78D4)边框;←/→ 直接切换 Tab,无需输入编号
 * - 输入框裸终端自绘,光标常驻文本尾(闪烁竖线);选择项高亮同为 #2B78D4
 * - 最后一个 Tab 汇总所有选择,Enter 生成
 * - 非 TTY 直接报错,提示走全参数模式
 */
public class Wizard {

    private static final String ESC = "\u001b";
    private static final String CLEAR = ESC + "[2J" + ESC + "[H";
    private static final String HOME = ESC + "[H";
    private static final String CLEAR_BELOW = ESC + "[J";
    private static final String BLUE = ESC + "[38;2;43;120;212m"; // #2B78D4
    private static final String BOLD = ESC + "[1m";
    private static final String RESET = ESC + "[0m";
    private static final String BLINK_BAR = ESC + "[5 q"; // DECSCUSR: 闪烁竖线光标

    // 特殊键码(避开可打印字符)
    private static final int UP = 1001, DOWN = 1002, RIGHT = 1003, LEFT = 1004;

    enum Type { INPUT, SELECT, CONFIRM }

    static class Field {
        final String tab;       // Tab 栏显示名
        final Type type;
        final String[][] options; // SELECT/CONFIRM: {id, 说明}
        String value;

        Field(String tab, Type type, String value, String[][] options) {
            this.tab = tab;
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
        fields.add(new Field("GroupId", Type.INPUT,
                groupId != null ? groupId : "com.example", null));
        fields.add(new Field("ArtifactId", Type.INPUT,
                artifactId != null ? artifactId : "demo", null));
        fields.add(new Field("工程预设", Type.SELECT,
                preset != null ? preset : "empty",
                new String[][]{{"empty", "空工程 —— 仅骨架,能跑起来"},
                        {"full", "完整后台(演示) —— 骨架 + 示例模块"}}));
        fields.add(new Field("HelloController 示例", Type.CONFIRM,
                withHello != null ? withHello.toString() : "true",
                new String[][]{{"true", "是"}, {"false", "否"}}));

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            if (terminal.getType().contains(Terminal.TYPE_DUMB) || System.console() == null) {
                throw new IOException("当前不是交互终端,请改用全参数模式: --group-id X --artifact-id Y --preset Z --with-hello true");
            }
            form(terminal, fields);
        }
        return Answers.of(fields.get(0).value, fields.get(1).value,
                fields.get(2).value, Boolean.parseBoolean(fields.get(3).value));
    }

    /** 单屏表单主循环:tab ∈ [0, fields.size()],最后一格是汇总确认页。 */
    private static void form(Terminal terminal, List<Field> fields) throws IOException {
        int summary = fields.size();
        int tab = 0;
        Attributes saved = terminal.enterRawMode();
        PrintWriter w = terminal.writer();
        try {
            w.print(CLEAR + BLINK_BAR);
            while (true) {
                render(w, fields, tab);
                int key = readKey(terminal);
                switch (key) {
                    case LEFT -> tab = Math.max(0, tab - 1);
                    case RIGHT -> tab = Math.min(summary, tab + 1);
                    case '\r', '\n' -> {
                        if (tab == summary) return; // 确认生成
                        tab++;
                    }
                    case UP, DOWN -> {
                        if (tab < summary) cycle(fields.get(tab), key == UP ? -1 : 1);
                    }
                    case 127, 8 -> {
                        Field f = tab < summary ? fields.get(tab) : null;
                        if (f != null && f.type == Type.INPUT && !f.value.isEmpty()) {
                            f.value = f.value.substring(0, f.value.length() - 1);
                        }
                    }
                    default -> {
                        Field f = tab < summary ? fields.get(tab) : null;
                        if (f != null && f.type == Type.INPUT && key >= 32 && key < 0x10000) {
                            f.value += (char) key;
                        }
                    }
                }
            }
        } finally {
            w.print(RESET + ESC + "[0 q"); // 恢复默认光标样式
            w.flush();
            terminal.setAttributes(saved);
        }
    }

    private static void render(PrintWriter w, List<Field> fields, int tab) {
        w.print(HOME + CLEAR_BELOW);
        w.println(BOLD + "JFast 原型 —— 生成一个 hello 级 Spring Boot 3 工程" + RESET);
        w.println();

        // Tab 栏
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i <= fields.size(); i++) {
            String name = i < fields.size() ? fields.get(i).tab : "确认";
            if (i == tab) {
                bar.append(BLUE).append("[ ").append(name).append(" ]").append(RESET).append("  ");
            } else {
                bar.append("  ").append(name).append("    ");
            }
        }
        w.println(bar);
        w.println();

        // 内容区
        if (tab < fields.size()) {
            Field f = fields.get(tab);
            switch (f.type) {
                case INPUT -> w.print("❯ " + f.value); // 真实光标留在文本尾,即闪烁输入光标
                case SELECT, CONFIRM -> {
                    for (String[] opt : f.options) {
                        boolean cur = opt[0].equals(f.value);
                        w.println(cur ? BLUE + "❯ " + opt[1] + RESET : "  " + opt[1]);
                    }
                }
            }
        } else {
            for (Field f : fields) {
                w.println("  " + f.tab + " = " + BLUE + display(f) + RESET);
            }
        }

        w.println();
        w.print("←/→ 切换 Tab · Enter " + (tab == fields.size() ? "确认生成" : "下一项")
                + (tab < fields.size() && fields.get(tab).type != Type.INPUT ? " · ↑/↓ 选择" : ""));
        w.flush();
    }

    private static void cycle(Field f, int delta) {
        int cur = 0;
        for (int i = 0; i < f.options.length; i++) if (f.options[i][0].equals(f.value)) cur = i;
        int next = Math.floorMod(cur + delta, f.options.length);
        f.value = f.options[next][0];
    }

    private static String display(Field f) {
        if (f.options != null) {
            for (String[] opt : f.options) if (opt[0].equals(f.value)) return opt[1];
        }
        return f.value;
    }

    /** 读一个键;方向键转义序列映射为特殊键码,不与可打印字符混淆。 */
    private static int readKey(Terminal terminal) throws IOException {
        var reader = terminal.reader();
        int ch = reader.read();
        if (ch == 27 && reader.peek(100) == '[') {
            reader.read(); // '['
            return switch (reader.read()) {
                case 'A' -> UP;
                case 'B' -> DOWN;
                case 'C' -> RIGHT;
                case 'D' -> LEFT;
                default -> 27;
            };
        }
        return ch;
    }
}
