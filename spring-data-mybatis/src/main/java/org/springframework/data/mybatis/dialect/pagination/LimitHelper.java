package org.springframework.data.mybatis.dialect.pagination;

import org.springframework.data.mybatis.dialect.RowSelection;

/**
 * A helper for dealing with LimitHandler implementations.
 * 
 * @author Jarvis Song
 */
public class LimitHelper {
	/**
	 * Is a max row limit indicated?
	 *
	 * @param selection The row selection options
	 * @return Whether a max row limit was indicated
	 */
	public static boolean hasMaxRows(RowSelection selection) {
		return selection != null && selection.getMaxRows() != null && selection.getMaxRows() > 0;
	}

	/**
	 * Should limit be applied?
	 *
	 * @param limitHandler The limit handler
	 * @param selection The row selection
	 * @return Whether limiting is indicated
	 */
	public static boolean useLimit(LimitHandler limitHandler, RowSelection selection) {
		return limitHandler.supportsLimit() && hasMaxRows(selection);
	}

	/**
	 * Is a first row limit indicated?
	 *
	 * @param selection The row selection options
	 * @return Whether a first row limit in indicated
	 */
	public static boolean hasFirstRow(RowSelection selection) {
		return getFirstRow(selection) > 0;
	}

	/**
	 * Retrieve the indicated first row for pagination
	 *
	 * @param selection The row selection options
	 * @return The first row
	 */
	public static int getFirstRow(RowSelection selection) {
		return (selection == null || selection.getFirstRow() == null) ? 0 : selection.getFirstRow();
	}

	private LimitHelper() {}
}
