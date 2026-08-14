package io.github.hhhrrr777.jfast.core;

/** 模板遍历/写盘失败(模板根缺失、IO 错误等)。 */
public final class TemplateWalkException extends RuntimeException {

    public TemplateWalkException(String message) {
        super(message);
    }

    public TemplateWalkException(String message, Throwable cause) {
        super(message, cause);
    }
}
