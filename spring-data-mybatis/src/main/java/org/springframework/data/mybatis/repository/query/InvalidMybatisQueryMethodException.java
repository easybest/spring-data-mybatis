package org.springframework.data.mybatis.repository.query;

public class InvalidMybatisQueryMethodException extends RuntimeException {

	public InvalidMybatisQueryMethodException() {
	}

	public InvalidMybatisQueryMethodException(String message) {
		super(message);
	}

	public InvalidMybatisQueryMethodException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidMybatisQueryMethodException(Throwable cause) {
		super(cause);
	}

	public InvalidMybatisQueryMethodException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
