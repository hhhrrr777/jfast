package io.jfast.proto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * 「目录即模板」渲染器:模板树镜像目标工程目录结构,walk 一遍逐个落地。
 * - 路径段里的 ${var} 用模型替换(包路径、类名等)
 * - .ftl 后缀的文件内容过 Freemarker,落地时去掉后缀;其余原样拷贝
 * - templates/base 恒生成,templates/full 仅在 preset=full 时叠加
 */
public class Generator {

    private static final Pattern PATH_VAR = Pattern.compile("\\$\\{(\\w+)}");

    private final Configuration fm = new Configuration(Configuration.VERSION_2_3_34);

    public void generate(Answers answers, Path target) throws Exception {
        fm.setDefaultEncoding("UTF-8");
        Map<String, Object> model = toModel(answers);
        renderTree("base", answers, model, target);
        if ("full".equals(answers.preset())) {
            renderTree("full", answers, model, target);
        }
    }

    private void renderTree(String layer, Answers answers, Map<String, Object> model, Path target)
            throws Exception {
        Path root = classpathTree("/templates/" + layer);
        try (Stream<Path> files = Files.walk(root)) {
            for (Path src : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                String rel = root.relativize(src).toString();
                String outRel = resolvePathVars(rel, model);
                boolean render = outRel.endsWith(".ftl");
                if (render) outRel = outRel.substring(0, outRel.length() - 4);
                // 条件文件的极简实现:hello 相关文件只在 withHello 时生成
                if (!answers.withHello() && outRel.contains("HelloController")) continue;
                Path out = target.resolve(outRel);
                Files.createDirectories(out.getParent());
                if (render) {
                    render(src, model, out);
                } else {
                    Files.copy(src, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("  create " + outRel);
            }
        }
    }

    /** classpath 上的模板树;打进 jar 后需要包一层 FileSystem 才能 walk(同一 jar 只能开一次)。 */
    private Path classpathTree(String name) throws Exception {
        URL url = getClass().getResource(name);
        if (url == null) throw new IllegalStateException("模板目录不存在: " + name);
        URI uri = url.toURI();
        if ("jar".equals(uri.getScheme())) {
            URI jarUri = URI.create(uri.toString().substring(0, uri.toString().indexOf('!')));
            FileSystem fs;
            try {
                fs = FileSystems.newFileSystem(jarUri, Map.of());
            } catch (FileSystemAlreadyExistsException e) {
                fs = FileSystems.getFileSystem(jarUri);
            }
            return fs.getPath(name);
        }
        return Paths.get(uri);
    }

    private void render(Path src, Map<String, Object> model, Path out) throws Exception {
        String content;
        try (InputStream in = Files.newInputStream(src)) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Template tpl = new Template(src.getFileName().toString(), new StringReader(content), fm);
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(out), StandardCharsets.UTF_8)) {
            tpl.process(model, w);
        }
    }

    private String resolvePathVars(String rel, Map<String, Object> model) {
        Matcher m = PATH_VAR.matcher(rel);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Object v = model.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? m.group(0) : v.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Map<String, Object> toModel(Answers a) {
        return Map.of(
                "groupId", a.groupId(),
                "artifactId", a.artifactId(),
                "packageName", a.packageName(),
                "packagePath", a.packagePath(),
                "appClassName", a.appClassName(),
                "bootVersion", a.bootVersion(),
                "withHello", a.withHello(),
                "preset", a.preset());
    }
}
