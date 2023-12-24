/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.easybest.mybatis.dialect.pagination;

import java.util.Locale;

import io.easybest.mybatis.dialect.AbstractPaginationHandler;
import io.easybest.mybatis.mapping.precompile.Segment;

/**
 * .
 *
 * @author Jarvis Song
 */

public class Oracle12PaginationHandler extends AbstractPaginationHandler {

	/**
	 * Singleton.
	 */
	public static final Oracle12PaginationHandler INSTANCE = new Oracle12PaginationHandler();

	@Override
	public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

		final boolean hasFirstRow = null != offset;
		final boolean hasMaxRows = null != fetchSize;

		if (!hasMaxRows) {
			return sql;
		}

		int forUpdateIndex = this.getForUpdateIndex(sql);

		String forUpdateClause = null;
		boolean isForUpdate = false;
		if (forUpdateIndex > -1) {
			forUpdateClause = sql.substring(forUpdateIndex);
			sql = sql.substring(0, forUpdateIndex - 1);
			isForUpdate = true;
		}

		final StringBuilder pagingSelect;

		final int forUpdateClauseLength;
		if (forUpdateClause == null) {
			forUpdateClauseLength = 0;
		}
		else {
			forUpdateClauseLength = forUpdateClause.length() + 1;
		}

		if (hasFirstRow) {
			pagingSelect = new StringBuilder(sql.length() + forUpdateClauseLength + 98);
			pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ");
			pagingSelect.append(sql);
			pagingSelect.append(" ) row_ where rownum &lt;= ").append(offsetEnd).append(") where rownum_ &gt; ")
					.append(offset);
		}
		else {
			pagingSelect = new StringBuilder(sql.length() + forUpdateClauseLength + 37);
			pagingSelect.append("select * from ( ");
			pagingSelect.append(sql);
			pagingSelect.append(" ) where rownum &lt;= ").append(offsetEnd);
		}

		if (isForUpdate) {
			pagingSelect.append(" ");
			pagingSelect.append(forUpdateClause);
		}

		return pagingSelect.toString();

	}

	private int getForUpdateIndex(String sql) {
		final int forUpdateLastIndex = sql.toLowerCase(Locale.ROOT).lastIndexOf("for update");
		final int lastIndexOfQuote = sql.lastIndexOf("'");
		if (forUpdateLastIndex > -1) {
			if (lastIndexOfQuote == -1) {
				return forUpdateLastIndex;
			}
			if (lastIndexOfQuote > forUpdateLastIndex) {
				return -1;
			}
			return forUpdateLastIndex;
		}
		return forUpdateLastIndex;
	}

}
