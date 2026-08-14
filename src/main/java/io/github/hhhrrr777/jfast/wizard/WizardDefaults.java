package io.github.hhhrrr777.jfast.wizard;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 默认值推导与公共校验规则(ADR-0006)。
 *
 * 所有方法均为纯函数,不依赖终端或 IO。
 */
public final class WizardDefaults {

    private static final Pattern PACKAGE_SEGMENT = Pattern.compile("[a-zA-Z_$][a-zA-Z0-9_$]*");
    private static final Pattern ARTIFACT_ID = Pattern.compile("[a-z][a-z0-9-]*");
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "record", "sealed", "permits", "var", "yield",
            "when");

    private WizardDefaults() {
    }

    public static List<String> jdkVersions() {
        return List.of("17", "21", "25");
    }

    public static List<Choice> databases() {
        return List.of(
                new Choice("mysql", "MySQL"),
                new Choice("postgresql", "PostgreSQL"),
                new Choice("dm", "达梦 DM"),
                new Choice("kingbase", "人大金仓 KingbaseES"),
                new Choice("opengauss", "openGauss"));
    }

    public static String defaultDbPort(String database) {
        return switch (database == null ? "" : database) {
            case "mysql" -> "3306";
            case "postgresql", "opengauss" -> "5432";
            case "dm" -> "5236";
            case "kingbase" -> "54321";
            default -> "3306";
        };
    }

    public static String deriveBasePackage(String groupId, String artifactId) {
        String normalizedArtifact = artifactId == null ? "" : artifactId.replace("-", "");
        if (groupId == null || groupId.isBlank()) {
            return "com.example." + normalizedArtifact;
        }
        return groupId + "." + normalizedArtifact;
    }

    public static String deriveDbName(String artifactId) {
        return artifactId == null ? "" : artifactId.replace("-", "_").toLowerCase();
    }

    /**
     * 从 artifactId 派生主类名前缀(大驼峰,合法 Java 标识符)。
     * 例:demo-app → DemoApp,jfast → Jfast。artifactId 已经过 validateArtifactId 校验,
     * 只含小写字母/数字/连字符,故派生结果必为合法类名前缀。
     */
    public static String deriveApplicationClass(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return "Application";
        }
        StringBuilder sb = new StringBuilder();
        for (String segment : artifactId.split("-")) {
            if (segment.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(segment.charAt(0)));
            if (segment.length() > 1) {
                sb.append(segment.substring(1));
            }
        }
        return sb.length() == 0 ? "Application" : sb.toString();
    }

    public static ValidationResult validatePackageName(String value) {
        if (value == null || value.isBlank()) {
            return ValidationResult.fail("包名不能为空");
        }
        String[] segments = value.split("\\.");
        if (segments.length < 2) {
            return ValidationResult.fail("包名至少需要两段,如 com.example");
        }
        for (String segment : segments) {
            if (segment.isEmpty()) {
                return ValidationResult.fail("包名不能包含空分段");
            }
            if (!PACKAGE_SEGMENT.matcher(segment).matches()) {
                return ValidationResult.fail("包名分段包含非法字符: " + segment);
            }
            if (JAVA_KEYWORDS.contains(segment)) {
                return ValidationResult.fail("包名分段不能使用 Java 关键字: " + segment);
            }
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validateArtifactId(String value) {
        if (value == null || value.isBlank()) {
            return ValidationResult.fail("artifactId 不能为空");
        }
        if (!ARTIFACT_ID.matcher(value).matches()) {
            return ValidationResult.fail("artifactId 只能包含小写字母、数字和连字符,且以小写字母开头");
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validateJdkVersion(String value) {
        if (!jdkVersions().contains(value)) {
            return ValidationResult.fail("JDK 版本必须是 17 / 21 / 25 之一");
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validateDatabase(String value) {
        if (databases().stream().noneMatch(c -> c.value().equals(value))) {
            return ValidationResult.fail("数据库类型不在可选列表中");
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validatePort(String value) {
        if (value == null || value.isBlank()) {
            return ValidationResult.fail("端口不能为空");
        }
        try {
            int port = Integer.parseInt(value);
            if (port < 1024 || port > 65535) {
                return ValidationResult.fail("端口需在 1024–65535 之间");
            }
            return ValidationResult.ok();
        } catch (NumberFormatException e) {
            return ValidationResult.fail("端口必须是整数");
        }
    }

    public static ValidationResult validateNonEmpty(String value) {
        if (value == null || value.isBlank()) {
            return ValidationResult.fail("此项必填");
        }
        return ValidationResult.ok();
    }
}
