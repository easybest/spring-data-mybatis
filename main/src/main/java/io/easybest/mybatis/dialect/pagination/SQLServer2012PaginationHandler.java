/*
 * Copyright 2019-2023 the original author or authors.
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

import io.easybest.mybatis.mapping.precompile.Segment;

/**
 * .
 *
 * @author Jarvis Song
 */
public class SQLServer2012PaginationHandler extends SQLServer2005PaginationHandler {

	@Override
	public String processSql(String sql, Segment offsetSegment, Segment fetchSize, Segment offsetEnd) {

		if (this.hasOrderBy(sql)) {
			if (null == fetchSize) {
				return sql;
			}
			return this.applyOffsetFetch(offsetSegment, fetchSize, offsetEnd, sql, this.getInsertPosition(sql));
		}

		return super.processSql(sql, offsetSegment, fetchSize, offsetEnd);
	}

	private String getOffsetFetch(Segment offsetSegment, Segment fetchSize, Segment offsetEnd) {
		if (null == offsetSegment) {
			return " offset 0 rows fetch next " + fetchSize + " rows only";
		}
		return " offset " + offsetSegment + " rows fetch next " + fetchSize + " rows only";
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

	private String applyOffsetFetch(Segment offsetSegment, Segment fetchSize, Segment offsetEnd, String sql,
			int position) {

		StringBuilder sb = new StringBuilder();
		sb.append(sql, 0, position);
		sb.append(this.getOffsetFetch(offsetSegment, fetchSize, offsetEnd));
		if (position > sql.length()) {
			sb.append(sql.substring(position - 1));
		}

		return sb.toString();
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
