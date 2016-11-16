package org.springframework.data.mybatis.repository.support;

/**
 * Created by songjiawei on 2016/11/13.
 */
public class MybatisQueryException extends RuntimeException {
    public MybatisQueryException() {
    }

    public MybatisQueryException(String message) {
        super(message);
    }

    public MybatisQueryException(String message, Throwable cause) {
        super(message, cause);
    }

    public MybatisQueryException(Throwable cause) {
        super(cause);
    }

    public MybatisQueryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
