package io.github.hhhrrr777.jfast.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 模板树遍历与落盘(ADR-0007):按层(base → overlay)遍历 classpath 资源模板树,
 * 支持开发期 classes 目录与 fat jar 内 walk;`.ftl` 后缀 opt-in 渲染并剥后缀,
 * 文件名/路径 `${}` 与内容同走渲染接缝、同一份模型;无后缀文件字节级拷贝;
 * 层间(含同层内)同输出路径撞车报错。
 *
 * 预设 overlay 根部的 `preset.yaml` 是 manifest 元数据(ADR-0004),非模板,
 * 遍历时跳过、不落盘——否则 empty 这类「仅 manifest 无模板」的预设会把
 * preset.yaml 拷进生成物。
 */
public final class FileTreeWalker {

    private static final String FTL_SUFFIX = ".ftl";

    /** 预设 manifest 文件名(ADR-0004),作为元数据处理、不拷入生成物。 */
    private static final String PRESET_MANIFEST = "preset.yaml";

    private final TemplateEngine engine;
    private final ClassLoader classLoader;

    public FileTreeWalker(TemplateEngine engine) {
        this(engine, FileTreeWalker.class.getClassLoader());
    }

    public FileTreeWalker(TemplateEngine engine, ClassLoader classLoader) {
        this.engine = engine;
        this.classLoader = classLoader;
    }

    /**
     * 按层叠加生成到输出目录。
     *
     * @param layerRoots 模板层根(classpath 资源路径,如 templates/base),按叠加顺序给出
     * @param outputDir  输出目录
     * @param model      根模型,文件名与内容共用
     * @throws TemplateCollisionException 两层产出同一输出路径
     * @throws TemplateWalkException      模板根缺失、协议不支持或 IO 失败
     * @throws TemplateRenderException    渲染失败(文件名或内容),信息带模板名 + 行号
     */
    public void generate(List<String> layerRoots, Path outputDir, Map<String, Object> model) {
        Map<String, String> origins = new LinkedHashMap<>();
        for (String root : layerRoots) {
            String normalizedRoot = stripTrailingSlash(root);
            for (String relativePath : listFiles(normalizedRoot)) {
                writeOne(normalizedRoot, relativePath, outputDir, model, origins);
            }
        }
    }

    private void writeOne(String root, String relativePath, Path outputDir,
                          Map<String, Object> model, Map<String, String> origins) {
        // 预设 manifest 是元数据而非模板,跳过不拷入生成物(ADR-0004)
        if (relativePath.equals(PRESET_MANIFEST)) {
            return;
        }
        String source = root + "/" + relativePath;
        // 文件名/路径与内容同走渲染接缝、同一份模型,先渲染再剥 .ftl 后缀
        String renderedPath = engine.render(relativePath, source + "(文件名)", model);
        String outputRelative = renderedPath.endsWith(FTL_SUFFIX)
                ? renderedPath.substring(0, renderedPath.length() - FTL_SUFFIX.length())
                : renderedPath;

        String previous = origins.putIfAbsent(outputRelative, source);
        if (previous != null) {
            throw new TemplateCollisionException(
                    "模板路径撞车: " + outputRelative + "(" + previous + " 与 " + source + ")");
        }

        Path target = outputDir.resolve(outputRelative);
        try {
            Files.createDirectories(target.getParent());
            byte[] bytes = readResource(source);
            if (renderedPath.endsWith(FTL_SUFFIX)) {
                String sourceText = new String(bytes, StandardCharsets.UTF_8);
                Files.writeString(target, engine.render(sourceText, source, model), StandardCharsets.UTF_8);
            } else {
                Files.write(target, bytes);
            }
        } catch (IOException e) {
            throw new TemplateWalkException("模板写盘失败: " + source + " -> " + target, e);
        }
    }

    private List<String> listFiles(String root) {
        URL url = classLoader.getResource(root);
        if (url == null) {
            throw new TemplateWalkException("模板根不存在或为空: " + root);
        }
        return switch (url.getProtocol()) {
            case "file" -> listFromFileSystem(url, root);
            case "jar" -> listFromJar(url, root);
            default -> throw new TemplateWalkException("不支持的模板根协议: " + url);
        };
    }

    private List<String> listFromFileSystem(URL url, String root) {
        try {
            Path rootPath = Path.of(url.toURI());
            try (var stream = Files.walk(rootPath)) {
                return stream.filter(Files::isRegularFile)
                        .map(rootPath::relativize)
                        .map(p -> p.toString().replace(p.getFileSystem().getSeparator(), "/"))
                        .sorted()
                        .toList();
            }
        } catch (URISyntaxException e) {
            throw new TemplateWalkException("模板根 URL 非法: " + url, e);
        } catch (IOException e) {
            throw new TemplateWalkException("模板根遍历失败: " + root, e);
        }
    }

    private List<String> listFromJar(URL url, String root) {
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            // 不用 classloader 共享缓存的 JarFile,拿到的实例归本方法所有,可安全关闭
            connection.setUseCaches(false);
            String prefix = root + "/";
            try (JarFile jar = connection.getJarFile()) {
                List<String> files = new ArrayList<>();
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().startsWith(prefix)) {
                        files.add(entry.getName().substring(prefix.length()));
                    }
                }
                files.sort(null);
                return files;
            }
        } catch (IOException e) {
            throw new TemplateWalkException("jar 内模板根遍历失败: " + root, e);
        }
    }

    private byte[] readResource(String source) {
        try (InputStream in = classLoader.getResourceAsStream(source)) {
            if (in == null) {
                throw new TemplateWalkException("模板资源不可读: " + source);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new TemplateWalkException("模板资源读取失败: " + source, e);
        }
    }

    private static String stripTrailingSlash(String root) {
        return root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
    }
}
