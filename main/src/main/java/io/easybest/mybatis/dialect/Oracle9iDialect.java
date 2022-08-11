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

package io.easybest.mybatis.dialect;

import java.util.Locale;

import io.easybest.mybatis.mapping.precompile.Segment;

/**
 * .
 *
 * @author Jarvis Song
 */
public class Oracle9iDialect extends Oracle8iDialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

			final boolean hasOffset = null != offset;

			sql = sql.trim();

			String forUpdateClause = null;
			boolean isForUpdate = false;
			final int forUpdateIndex = sql.toLowerCase(Locale.ROOT).lastIndexOf("for update");
			if (forUpdateIndex > -1) {
				forUpdateClause = sql.substring(forUpdateIndex);
				sql = sql.substring(0, forUpdateIndex - 1);
				isForUpdate = true;
			}

			final StringBuilder pagingSelect = new StringBuilder(sql.length() + 100);
			if (hasOffset) {
				pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ");
			}
			else {
				pagingSelect.append("select * from ( ");
			}
			pagingSelect.append(sql);
			if (hasOffset) {
				pagingSelect.append(" ) row_ where rownum &lt;= ").append(offsetEnd).append(") where rownum_ &gt; ")
						.append(offset);
			}
			else {
				pagingSelect.append(" ) where rownum &lt;= ").append(offsetEnd);
			}

			if (isForUpdate) {
				pagingSelect.append(" ");
				pagingSelect.append(forUpdateClause);
			}

			return pagingSelect.toString();

		}
	};

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

}
