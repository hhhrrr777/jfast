package io.github.hhhrrr777.jfast.wizard;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 问题树声明(ADR-0006)。
 *
 * @param id          问题标识
 * @param displayName 向导显示名
 * @param tab         所属 Tab(工程坐标 / 数据库 / 运行)
 * @param defaultValue 动态默认值,依赖已采集答案
 * @param choices     可选值列表(非选择题为空)
 * @param validator   校验器
 */
public record Question(QuestionId id,
                       String displayName,
                       String tab,
                       Function<Answers, String> defaultValue,
                       List<Choice> choices,
                       Validator validator) {

    public boolean isChoice() {
        return choices != null && !choices.isEmpty();
    }

    public String defaultFor(Answers answers) {
        return defaultValue == null ? "" : defaultValue.apply(answers);
    }

    public ValidationResult validate(String value) {
        if (validator == null) {
            return ValidationResult.ok();
        }
        return validator.validate(value);
    }
}
