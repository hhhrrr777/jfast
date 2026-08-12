package io.jfast.proto;

/**
 * 向导收集到的答案,同时就是渲染模板用的模型。
 */
public record Answers(
        String groupId,
        String artifactId,
        String packageName,
        String packagePath,
        String appClassName,
        String preset,
        boolean withHello,
        String bootVersion) {

    static Answers of(String groupId, String artifactId, String preset, boolean withHello) {
        String packageName = groupId + "." + artifactId.replace("-", "");
        String appClassName = toPascalCase(artifactId) + "Application";
        return new Answers(groupId, artifactId, packageName, packageName.replace('.', '/'),
                appClassName, preset, withHello, "3.5.0");
    }

    private static String toPascalCase(String s) {
        StringBuilder sb = new StringBuilder();
        for (String part : s.split("[-_]")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }
}
