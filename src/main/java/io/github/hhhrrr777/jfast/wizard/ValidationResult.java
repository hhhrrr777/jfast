package io.github.hhhrrr777.jfast.wizard;

/**
 * 字段校验结果。
 */
public record ValidationResult(boolean valid, String errorMessage) {

    public static ValidationResult ok() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult fail(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }
}
