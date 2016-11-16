package org.springframework.data.mybatis.repository.support;

/**
 * Created by songjiawei on 2016/11/13.
 */
public class MybatisRepositoryCreationException extends RuntimeException {
    public MybatisRepositoryCreationException() {
    }

    public MybatisRepositoryCreationException(String message) {
        super(message);
    }

    public MybatisRepositoryCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MybatisRepositoryCreationException(Throwable cause) {
        super(cause);
    }

    public MybatisRepositoryCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
