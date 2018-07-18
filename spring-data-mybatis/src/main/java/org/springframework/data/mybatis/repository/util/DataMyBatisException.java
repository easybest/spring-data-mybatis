package org.springframework.data.mybatis.repository.util;

/**
 * @author Jarvis Song
 */
public class DataMyBatisException extends RuntimeException {

	public DataMyBatisException() {}

	public DataMyBatisException(String message) {
		super(message);
	}

	public DataMyBatisException(String message, Throwable cause) {
		super(message, cause);
	}

	public DataMyBatisException(Throwable cause) {
		super(cause);
	}

	public DataMyBatisException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
