package io.github.hhhrrr777.jfast.wizard;

/**
 * 字段值校验器。
 */
@FunctionalInterface
public interface Validator {
    ValidationResult validate(String value);
}
