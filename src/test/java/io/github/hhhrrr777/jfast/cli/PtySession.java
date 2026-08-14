package io.github.hhhrrr777.jfast.cli;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * pty4j 伪终端会话驱动(PTY golden-path 冒烟测试基础设施)。
 *
 * 启动子进程并为其绑定一个真实 PTY,按输出内容驱动按键输入,
 * 断言最终输出与退出码。PTY 冒烟用于防「逻辑对但 UI 卡死」(ADR-0009 T6)。
 *
 * 后台读取线程持续阻塞读取子进程 stdout,waitFor 只轮询累积缓冲,
 * 避免「先查 available 再读」与「直接阻塞读」两种方式的坑。
 */
final class PtySession implements AutoCloseable {

    private static final long DEFAULT_TIMEOUT_MILLIS = 30_000;
    // CSI(光标/颜色/擦除)、OSC(标题)、字符集选择与单字符控制、ST
    private static final Pattern ANSI_ESCAPE = Pattern.compile(
            "\\x1B\\[[0-9;?]*[ -/]*[@-~]"
                    + "|\\x1B\\][^\\x07]*(?:\\x07|\\x1B\\\\)"
                    + "|\\x1B[()=<>A-Za-z]"
                    + "|\\x1B\\\\");

    private final PtyProcess process;
    private final OutputStream stdin;
    private final Thread readerThread;
    private final StringBuilder raw = new StringBuilder();
    private final AtomicReference<IOException> readFailure = new AtomicReference<>();
    private long deadline;

    private PtySession(PtyProcess process) {
        this.process = process;
        this.stdin = process.getOutputStream();
        this.deadline = System.currentTimeMillis() + DEFAULT_TIMEOUT_MILLIS;
        this.readerThread = new Thread(() -> {
            try (InputStream stdout = process.getInputStream()) {
                byte[] chunk = new byte[4096];
                int read;
                while ((read = stdout.read(chunk)) != -1) {
                    synchronized (raw) {
                        raw.append(new String(chunk, 0, read, StandardCharsets.UTF_8));
                    }
                }
            } catch (IOException e) {
                readFailure.set(e);
            }
        }, "pty-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    /** 启动 `java ... Main new` 于指定目录,TERM 设为 xterm 以启用真实终端 provider。 */
    static PtySession launch(Path workingDir, String... extraArgs) throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + "/bin/java";
        String classpath = System.getProperty("java.class.path");

        String[] command = new String[extraArgs.length + 5];
        command[0] = javaBin;
        command[1] = "-cp";
        command[2] = classpath;
        command[3] = Main.class.getName();
        command[4] = "new";
        System.arraycopy(extraArgs, 0, command, 5, extraArgs.length);

        // LANG 必须显式固定:CI runner 默认 POSIX/C locale,子进程会按 ASCII 编码
        // 输出,中文提示全部退化为 ?,按输出内容驱动的断言随之超时
        PtyProcess process = new PtyProcessBuilder()
                .setCommand(command)
                .setEnvironment(Map.of("TERM", "xterm", "LANG", "en_US.UTF-8"))
                .setDirectory(workingDir.toString())
                .setInitialColumns(120)
                .setInitialRows(40)
                .start();
        return new PtySession(process);
    }

    /** 读取输出直到指定标记出现(剥除 ANSI 转义后匹配)。 */
    String waitFor(String marker) throws IOException, InterruptedException {
        while (true) {
            String plain;
            synchronized (raw) {
                plain = plainOf(raw);
            }
            if (plain.contains(marker)) {
                return plain;
            }
            IOException failure = readFailure.get();
            if (failure != null) {
                throw failure;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError(
                        "等待输出标记超时: " + marker + "\n已收到输出:\n" + plain);
            }
            Thread.sleep(50);
        }
    }

    void sendLine(String text) throws IOException {
        stdin.write((text + "\r").getBytes(StandardCharsets.UTF_8));
        stdin.flush();
    }

    void sendEnter() throws IOException {
        sendLine("");
    }

    int exitCode() throws InterruptedException {
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("子进程未在 5 秒内退出");
        }
        return process.exitValue();
    }

    String errorOutput() throws IOException {
        try (InputStream stderr = process.getErrorStream()) {
            return new String(stderr.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String plainOf(StringBuilder raw) {
        return ANSI_ESCAPE.matcher(raw).replaceAll("");
    }

    @Override
    public void close() {
        process.destroyForcibly();
        readerThread.interrupt();
    }
}
