package org.springframework.data.mybatis.dialect.pagination;

import org.springframework.data.mybatis.dialect.RowSelection;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Default implementation of {@link LimitHandler} interface.
 */
public abstract class AbstractLimitHandler implements LimitHandler {
	protected AbstractLimitHandler() {
		// NOP
	}

	@Override
	public boolean supportsLimit() {
		return false;
	}

	@Override
	public boolean supportsLimitOffset() {
		return supportsLimit();
	}

	/**
	 * Does this handler support bind variables (i.e., prepared statement parameters) for its limit/offset?
	 *
	 * @return True if bind variables can be used; false otherwise.
	 */
	public boolean supportsVariableLimit() {
		return supportsLimit();
	}

	/**
	 * ANSI SQL defines the LIMIT clause to be in the form LIMIT offset, limit. Does this dialect require us to bind the
	 * parameters in reverse order?
	 *
	 * @return true if the correct order is limit, offset
	 */
	public boolean bindLimitParametersInReverseOrder() {
		return false;
	}

	/**
	 * Does the <tt>LIMIT</tt> clause come at the start of the <tt>SELECT</tt> statement, rather than at the end?
	 *
	 * @return true if limit parameters should come before other parameters
	 */
	public boolean bindLimitParametersFirst() {
		return false;
	}

	/**
	 * Does the <tt>LIMIT</tt> clause take a "maximum" row number instead of a total number of returned rows?
	 * This is easiest understood via an example. Consider you have a table with 20 rows, but you only want to retrieve
	 * rows number 11 through 20. Generally, a limit with offset would say that the offset = 11 and the limit = 10 (we
	 * only want 10 rows at a time); this is specifying the total number of returned rows. Some dialects require that we
	 * instead specify offset = 11 and limit = 20, where 20 is the "last" row we want relative to offset (i.e. total
	 * number of rows = 20 - 11 = 9)
	 * So essentially, is limit relative from offset? Or is limit absolute?
	 *
	 * @return True if limit is relative from offset; false otherwise.
	 */
	public boolean useMaxForLimit() {
		return false;
	}

	/**
	 * Generally, if there is no limit applied to a query we do not apply any limits to the SQL query. This option forces
	 * that the limit be written to the SQL query.
	 *
	 * @return True to force limit into SQL query even if none specified in query; false otherwise.
	 */
	public boolean forceLimitUsage() {
		return false;
	}

	/**
	 * Query APIs explicitly state that setFirstResult() should be a zero-based offset. Here we allow the Dialect a chance
	 * to convert that value based on what the underlying db or driver will expect.
	 * 
	 * NOTE: what gets passed into {@link AbstractLimitHandler#processSql(String, RowSelection)} is the zero-based offset.
	 * Dialects which do not {@link #supportsVariableLimit} should take care to perform any needed first-row-conversion
	 * calls prior to injecting the limit values into the SQL string.
	 *
	 * @param zeroBasedFirstResult The user-supplied, zero-based first row offset.
	 * @return The corresponding db/dialect specific offset.
	 */
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		throw new UnsupportedOperationException("Paged queries not supported by " + getClass().getName());
	}

	@Override
	public int bindLimitParametersAtStartOfQuery(RowSelection selection, PreparedStatement statement, int index)
			throws SQLException {
		return bindLimitParametersFirst() ? bindLimitParameters(selection, statement, index) : 0;
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(RowSelection selection, PreparedStatement statement, int index)
			throws SQLException {
		return !bindLimitParametersFirst() ? bindLimitParameters(selection, statement, index) : 0;
	}

	@Override
	public void setMaxRows(RowSelection selection, PreparedStatement statement) throws SQLException {}

	/**
	 * Default implementation of binding parameter values needed by the LIMIT clause.
	 *
	 * @param selection the selection criteria for rows.
	 * @param statement Statement to which to bind limit parameter values.
	 * @param index Index from which to start binding.
	 * @return The number of parameter values bound.
	 * @throws SQLException Indicates problems binding parameter values.
	 */
	protected final int bindLimitParameters(RowSelection selection, PreparedStatement statement, int index)
			throws SQLException {
		if (!supportsVariableLimit() || !LimitHelper.hasMaxRows(selection)) {
			return 0;
		}
		final int firstRow = convertToFirstRowValue(LimitHelper.getFirstRow(selection));
		final int lastRow = getMaxOrLimit(selection);
		final boolean hasFirstRow = supportsLimitOffset() && (firstRow > 0 || forceLimitUsage());
		final boolean reverse = bindLimitParametersInReverseOrder();
		if (hasFirstRow) {
			statement.setInt(index + (reverse ? 1 : 0), firstRow);
		}
		statement.setInt(index + (reverse || !hasFirstRow ? 0 : 1), lastRow);
		return hasFirstRow ? 2 : 1;
	}

	/**
	 * Some dialect-specific LIMIT clauses require the maximum last row number (aka, first_row_number + total_row_count),
	 * while others require the maximum returned row count (the total maximum number of rows to return).
	 *
	 * @param selection the selection criteria for rows.
	 * @return The appropriate value to bind into the limit clause.
	 */
	protected final int getMaxOrLimit(RowSelection selection) {
		final int firstRow = convertToFirstRowValue(LimitHelper.getFirstRow(selection));
		final int lastRow = selection.getMaxRows();
		return useMaxForLimit() ? lastRow + firstRow : lastRow;
	}
}
