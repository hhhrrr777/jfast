package io.github.hhhrrr777.jfast.core;

/** base/overlay 同路径撞车(ADR-0004:纯叠加,不可覆盖/删除 base 文件)。 */
public final class TemplateCollisionException extends RuntimeException {

    public TemplateCollisionException(String message) {
        super(message);
    }
}
