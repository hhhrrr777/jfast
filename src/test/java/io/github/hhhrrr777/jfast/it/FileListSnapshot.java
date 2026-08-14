package io.github.hhhrrr777.jfast.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件清单快照断言(ADR-0009 T2)。
 *
 * 每预设一份 golden 清单(src/test/resources/snapshots/&lt;preset&gt;.filelist),
 * 只记录相对路径、一行一个,捕捉「多了/少了文件」这类回归;内容层面的回归由
 * 定点断言覆盖,不做全量内容快照。
 *
 * 更新走显式开关:`mvn test -DupdateSnapshots`,diff 随 PR 人工 review。
 * 快照缺失而开关未开时报错并提示如何生成,避免静默 accept。
 */
final class FileListSnapshot {

    static final String UPDATE_PROPERTY = "updateSnapshots";

    private static final Path SNAPSHOT_DIR = Path.of("src/test/resources/snapshots");

    private FileListSnapshot() {
    }

    static boolean updateEnabled() {
        String value = System.getProperty(UPDATE_PROPERTY);
        // `-DupdateSnapshots`(无值,空串)与 `-DupdateSnapshots=true` 均视为打开
        return value != null && !"false".equalsIgnoreCase(value);
    }

    /** 生成目录的文件清单:相对路径、正斜杠、排序、一行一个。 */
    static String listing(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(p -> p.toString().replace(p.getFileSystem().getSeparator(), "/"))
                    .sorted()
                    .collect(Collectors.joining("\n", "", "\n"));
        } catch (IOException e) {
            throw new UncheckedIOException("遍历生成物失败: " + root, e);
        }
    }

    /**
     * 断言生成物文件清单与快照一致;开关打开时改为重写快照。
     *
     * @return 实际清单(供失败信息使用)
     */
    static String assertMatches(String snapshotName, Path generatedRoot) {
        String actual = listing(generatedRoot);
        Path snapshotFile = SNAPSHOT_DIR.resolve(snapshotName);

        if (updateEnabled()) {
            write(snapshotFile, actual);
            return actual;
        }

        if (!Files.exists(snapshotFile)) {
            throw new AssertionError("快照不存在: " + snapshotFile.toAbsolutePath()
                    + "\n先运行 mvn test -DupdateSnapshots 生成快照,再将 diff 随 PR review。");
        }
        String expected = read(snapshotFile);
        if (!expected.equals(actual)) {
            throw new AssertionError("文件清单快照不一致: " + snapshotName
                    + "\n快照: " + snapshotFile.toAbsolutePath()
                    + "\n生成物: " + generatedRoot.toAbsolutePath()
                    + "\n--- 快照(期望) ---\n" + expected
                    + "\n--- 实际 ---\n" + actual
                    + "\n确认变更符合预期后,运行 mvn test -DupdateSnapshots 更新快照。");
        }
        return actual;
    }

    private static String read(Path file) {
        try {
            // 归一化行尾:Windows checkout 可能为 CRLF(ADR-0009 nightly Windows 矩阵),
            // 与 listing() 固定的 \n 比较会产生假阳性
            return Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException("读取快照失败: " + file, e);
        }
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("写快照失败: " + file, e);
        }
    }
}
