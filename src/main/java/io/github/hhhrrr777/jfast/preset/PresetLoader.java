package io.github.hhhrrr777.jfast.preset;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * 从 classpath 扫描并加载预设 manifest(preset.yaml)。
 *
 * ADR-0004 约定:所有预设统一放在 templates/presets/<name>/preset.yaml,
 * 字段仅限 name / displayName / description / questions / conditions,未知字段报错。
 */
public final class PresetLoader {

    private static final String PRESETS_ROOT = "templates/presets";

    private final ClassLoader classLoader;

    public PresetLoader() {
        this(PresetLoader.class.getClassLoader());
    }

    public PresetLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /** 加载全部可用预设,按目录名字母序稳定排列。 */
    public List<Preset> loadAll() {
        List<String> names = scanPresetNames();
        List<Preset> presets = new ArrayList<>();
        for (String name : names) {
            presets.add(load(name));
        }
        return presets;
    }

    /** 按名称加载单个预设。 */
    public Preset load(String name) {
        String path = PRESETS_ROOT + "/" + name + "/preset.yaml";
        try (InputStream in = classLoader.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("预设不存在: " + name);
            }
            return parse(name, in);
        } catch (IOException e) {
            throw new IllegalStateException("读取 preset.yaml 失败: " + name, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Preset parse(String name, InputStream in) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> raw = yaml.load(in);
        if (raw == null) {
            throw new IllegalArgumentException("preset.yaml 为空: " + name);
        }

        validateKeys(name, raw.keySet());

        String displayName = string(raw, "displayName", name);
        String description = string(raw, "description", "");
        List<String> questions = list(raw, "questions");
        Map<String, Object> conditions = map(raw, "conditions");

        return new Preset(name, displayName, description, questions, conditions);
    }

    private void validateKeys(String name, java.util.Set<String> keys) {
        List<String> allowed = List.of("name", "displayName", "description", "questions", "conditions");
        for (String key : keys) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException(
                        "预设 " + name + " 的 preset.yaml 包含未知字段: " + key);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("preset.yaml 字段 " + key + " 必须是对象");
        }
        return new LinkedHashMap<>((Map<String, Object>) value);
    }

    @SuppressWarnings("unchecked")
    private List<String> list(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        if (value == null) {
            throw new IllegalArgumentException("preset.yaml 缺少必填字段: " + key);
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("preset.yaml 字段 " + key + " 必须是列表");
        }
        List<?> list = (List<?>) value;
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String)) {
                throw new IllegalArgumentException("preset.yaml 字段 " + key + " 必须全是字符串");
            }
            result.add((String) item);
        }
        return result;
    }

    private String string(Map<String, Object> raw, String key, String defaultValue) {
        Object value = raw.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private List<String> scanPresetNames() {
        // 开发期从文件系统扫描,打包后从 jar 内扫描
        URL url = classLoader.getResource(PRESETS_ROOT);
        if (url == null) {
            throw new IllegalStateException("模板预设根不存在: " + PRESETS_ROOT);
        }
        return switch (url.getProtocol()) {
            case "file" -> scanFromFileSystem(url);
            case "jar" -> scanFromJar(url);
            default -> throw new IllegalStateException("不支持的预设根协议: " + url);
        };
    }

    private List<String> scanFromFileSystem(java.net.URL url) {
        try {
            java.nio.file.Path root = java.nio.file.Path.of(url.toURI());
            List<String> names = new ArrayList<>();
            try (var stream = java.nio.file.Files.list(root)) {
                stream.filter(java.nio.file.Files::isDirectory)
                        .map(p -> p.getFileName().toString())
                        .sorted()
                        .forEach(names::add);
            }
            return names;
        } catch (Exception e) {
            throw new IllegalStateException("扫描预设目录失败: " + url, e);
        }
    }

    private List<String> scanFromJar(java.net.URL url) {
        try {
            java.net.JarURLConnection connection = (java.net.JarURLConnection) url.openConnection();
            connection.setUseCaches(false);
            String prefix = PRESETS_ROOT + "/";
            try (java.util.jar.JarFile jar = connection.getJarFile()) {
                java.util.Set<String> names = new java.util.TreeSet<>();
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (entry.isDirectory() && entryName.startsWith(prefix) && !entryName.equals(prefix)) {
                        String rest = entryName.substring(prefix.length());
                        if (!rest.contains("/")) {
                            names.add(rest);
                        }
                    }
                }
                return new ArrayList<>(names);
            }
        } catch (IOException e) {
            throw new IllegalStateException("从 jar 扫描预设失败: " + url, e);
        }
    }
}
