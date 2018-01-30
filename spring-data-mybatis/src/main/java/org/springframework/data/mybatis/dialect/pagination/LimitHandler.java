package org.springframework.data.mybatis.dialect.pagination;

import org.springframework.data.mybatis.dialect.RowSelection;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Contract defining dialect-specific LIMIT clause handling. Typically implementers might consider extending
 * {@link AbstractLimitHandler} class.
 *
 * @author Jarvis Song
 */
public interface LimitHandler {
	/**
	 * Does this handler support some form of limiting query results via a SQL clause?
	 *
	 * @return True if this handler supports some form of LIMIT.
	 */
	boolean supportsLimit();

	/**
	 * Does this handler's LIMIT support (if any) additionally support specifying an offset?
	 *
	 * @return True if the handler supports an offset within the limit support.
	 */
	boolean supportsLimitOffset();

	/**
	 * Return processed SQL query.
	 *
	 * @param sql the SQL query to process.
	 * @param selection the selection criteria for rows.
	 * @return Query statement with LIMIT clause applied.
	 */
	String processSql(String sql, RowSelection selection);

	/**
	 * Bind parameter values needed by the LIMIT clause before original SELECT statement.
	 *
	 * @param selection the selection criteria for rows.
	 * @param statement Statement to which to bind limit parameter values.
	 * @param index Index from which to start binding.
	 * @return The number of parameter values bound.
	 * @throws SQLException Indicates problems binding parameter values.
	 */
	int bindLimitParametersAtStartOfQuery(RowSelection selection, PreparedStatement statement, int index)
			throws SQLException;

	/**
	 * Bind parameter values needed by the LIMIT clause after original SELECT statement.
	 *
	 * @param selection the selection criteria for rows.
	 * @param statement Statement to which to bind limit parameter values.
	 * @param index Index from which to start binding.
	 * @return The number of parameter values bound.
	 * @throws SQLException Indicates problems binding parameter values.
	 */
	int bindLimitParametersAtEndOfQuery(RowSelection selection, PreparedStatement statement, int index)
			throws SQLException;

	/**
	 * Use JDBC API to limit the number of rows returned by the SQL query. Typically handlers that do not support LIMIT
	 * clause should implement this method.
	 *
	 * @param selection the selection criteria for rows.
	 * @param statement Statement which number of returned rows shall be limited.
	 * @throws SQLException Indicates problems while limiting maximum rows returned.
	 */
	void setMaxRows(RowSelection selection, PreparedStatement statement) throws SQLException;
}
