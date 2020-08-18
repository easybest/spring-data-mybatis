/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mybatis.dialect.pagination;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class SQLServer2012LimitHandler extends SQLServer2005LimitHandler {

	private boolean usedOffsetFetch;

	@Override
	public String processSql(String sql, RowSelection selection) {

		if (this.hasOrderBy(sql)) {
			if (!LimitHelper.useLimit(this, selection)) {
				return sql;
			}
			return this.applyOffsetFetch(selection, sql, this.getInsertPosition(sql));
		}

		return super.processSql(sql, selection);
	}

	private String applyOffsetFetch(RowSelection selection, String sql, int position) {
		this.usedOffsetFetch = true;

		StringBuilder sb = new StringBuilder();
		sb.append(sql, 0, position);
		sb.append(this.getOffsetFetch(selection));
		if (position > sql.length()) {
			sb.append(sql.substring(position - 1));
		}

		return sb.toString();
	}

	private int getInsertPosition(String sql) {
		int position = sql.length() - 1;
		for (; position > 0; --position) {
			char ch = sql.charAt(position);
			if (ch != ';' && ch != ' ' && ch != '\r' && ch != '\n') {
				break;
			}
		}
		return position + 1;
	}

	private String getOffsetFetch(RowSelection selection) {
		if (!LimitHelper.hasFirstRow(selection)) {
			return " offset 0 rows fetch next " + selection.getMaxRows() + " rows only";
		}
		return " offset " + LimitHelper.getFirstRow(selection) + " rows fetch next " + selection.getMaxRows()
				+ " rows only";
	}

	private boolean hasOrderBy(String sql) {
		int depth = 0;
		String lowerCaseSQL = sql.toLowerCase();
		for (int i = lowerCaseSQL.length() - 1; i >= 0; --i) {
			char ch = lowerCaseSQL.charAt(i);
			if (ch == '(') {
				depth++;
			}
			else if (ch == ')') {
				depth--;
			}
			if (depth == 0) {
				if (lowerCaseSQL.startsWith("order by ", i)) {
					return true;
				}
			}
		}
		return false;
	}

}
