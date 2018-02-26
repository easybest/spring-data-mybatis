package org.springframework.data.mybatis.dialect;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * @author Jarvis Song
 */
public class DialectException extends UncategorizedDataAccessException {
	/**
	 * Constructor for UncategorizedDataAccessException.
	 *
	 * @param msg the detail message
	 * @param cause the exception thrown by underlying data access API
	 */
	public DialectException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
