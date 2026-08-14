package io.github.hhhrrr777.jfast.core;

/** 渲染失败(未定义变量、语法错误等);信息带模板名 + 行号。 */
public final class TemplateRenderException extends RuntimeException {

    public TemplateRenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
