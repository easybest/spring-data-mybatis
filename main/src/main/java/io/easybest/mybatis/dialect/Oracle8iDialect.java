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
public class Oracle8iDialect extends AbstractDialect {

	private static final AbstractPaginationHandler PAGINATION_HANDLER = new AbstractPaginationHandler() {

		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

			final boolean hasOffset = null != offset;

			sql = sql.trim();

			boolean isForUpdate = false;
			if (sql.toLowerCase(Locale.ROOT).endsWith(" for update")) {
				sql = sql.substring(0, sql.length() - 11);
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
				pagingSelect.append(" ) row_ ) where rownum_ &lt;= ").append(offsetEnd).append(" and rownum_ &gt; ")
						.append(offset);
			}
			else {
				pagingSelect.append(" ) where rownum &lt;= ").append(offsetEnd);
			}

			if (isForUpdate) {
				pagingSelect.append(" for update");
			}

			return pagingSelect.toString();

		}
	};

	@Override
	public PaginationHandler getPaginationHandler() {
		return PAGINATION_HANDLER;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {

		return "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
	}

	@Override
	public boolean supportsBoolean() {
		return false;
	}

}
