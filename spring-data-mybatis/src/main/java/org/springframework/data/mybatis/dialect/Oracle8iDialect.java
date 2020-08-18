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
package org.springframework.data.mybatis.dialect;

import java.util.Locale;

import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class Oracle8iDialect extends Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow(selection);
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
				pagingSelect.append(" ) row_ ) where rownum_ &lt;= " + selection.getLastRow() + " and rownum_ &gt; "
						+ selection.getFirstRow());
			}
			else {
				pagingSelect.append(" ) where rownum &lt;= " + selection.getLastRow());
			}

			if (isForUpdate) {
				pagingSelect.append(" for update");
			}

			return pagingSelect.toString();
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}
	};

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString(sequenceName) + " from dual";
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

}
