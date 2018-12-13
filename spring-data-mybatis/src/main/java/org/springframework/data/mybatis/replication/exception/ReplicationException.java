package org.springframework.data.mybatis.replication.exception;

/**
 * replication exception.
 *
 * @author JARVIS SONG
 */
public class ReplicationException extends RuntimeException {

	public ReplicationException() {
	}

	public ReplicationException(String message) {
		super(message);
	}

	public ReplicationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReplicationException(Throwable cause) {
		super(cause);
	}

	public ReplicationException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
