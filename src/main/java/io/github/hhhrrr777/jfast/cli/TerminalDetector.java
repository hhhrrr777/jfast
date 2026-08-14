package io.github.hhhrrr777.jfast.cli;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * 终端能力检测(ADR-0001):非 TTY 或 dumb terminal 强制走全参数模式。
 *
 * 封装 {@link TerminalBuilder} 的副作用,便于测试注入替代实现。
 */
public final class TerminalDetector {

    private TerminalDetector() {
    }

    /**
     * 判断当前环境是否支持交互式向导。
     *
     * 先通过 {@link System#console()} 与 TERM 环境变量做快速判断;
     * 若看起来是终端,再用 JLine 确认终端类型不是 dumb。
     */
    public static boolean isInteractive() {
        if (System.console() == null) {
            return false;
        }
        String term = System.getenv("TERM");
        if ("dumb".equalsIgnoreCase(term)) {
            return false;
        }
        try (Terminal terminal = TerminalBuilder.terminal()) {
            return !"dumb".equalsIgnoreCase(terminal.getType());
        } catch (Exception e) {
            return false;
        }
    }
}
