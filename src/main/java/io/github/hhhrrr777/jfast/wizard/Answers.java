package io.github.hhhrrr777.jfast.wizard;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 问答会话中已采集的答案,按问题顺序保留。
 *
 * 逻辑层不依赖终端,答案来源可以是 picocli 参数、向导交互或测试直接构造。
 */
public final class Answers {

    private final Map<QuestionId, String> values;

    private Answers(Map<QuestionId, String> values) {
        this.values = new LinkedHashMap<>(values);
    }

    public static Answers empty() {
        return new Answers(Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Answers with(QuestionId id, String value) {
        Map<QuestionId, String> copy = new LinkedHashMap<>(values);
        copy.put(id, value);
        return new Answers(copy);
    }

    public Optional<String> get(QuestionId id) {
        return Optional.ofNullable(values.get(id));
    }

    public String getOrDefault(QuestionId id, String defaultValue) {
        return values.getOrDefault(id, defaultValue);
    }

    public boolean contains(QuestionId id) {
        return values.containsKey(id);
    }

    public Map<QuestionId, String> asMap() {
        return Collections.unmodifiableMap(values);
    }

    public static final class Builder {
        private final Map<QuestionId, String> values = new LinkedHashMap<>();

        public Builder set(QuestionId id, String value) {
            this.values.put(id, value);
            return this;
        }

        public Answers build() {
            return new Answers(values);
        }
    }
}
