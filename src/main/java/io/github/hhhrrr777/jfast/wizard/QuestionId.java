package io.github.hhhrrr777.jfast.wizard;

/**
 * 问题标识。
 *
 * 与 preset.yaml 中 questions 白名单的字符串一一对应。
 */
public enum QuestionId {
    PRESET("preset"),
    GROUP_ID("groupId"),
    ARTIFACT_ID("artifactId"),
    BASE_PACKAGE("basePackage"),
    JDK_VERSION("jdkVersion"),
    DATABASE("database"),
    DB_HOST("dbHost"),
    DB_PORT("dbPort"),
    DB_NAME("dbName"),
    DB_USER("dbUser"),
    DB_PASSWORD("dbPassword"),
    SERVER_PORT("serverPort");

    private final String key;

    QuestionId(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** CLI 参数名(kebab-case),如 groupId → --group-id。 */
    public String paramName() {
        return key.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    public static QuestionId fromKey(String key) {
        for (QuestionId id : values()) {
            if (id.key.equals(key)) {
                return id;
            }
        }
        throw new IllegalArgumentException("未知问题: " + key);
    }
}
