package io.github.hhhrrr777.jfast.baseline.common.exception;

/**
 * 业务异常:Service 层抛出,由全局异常处理器转为统一响应。
 */
public class ServiceException extends RuntimeException {

    private final Integer code;

    public ServiceException(String message) {
        this(message, null);
    }

    public ServiceException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
