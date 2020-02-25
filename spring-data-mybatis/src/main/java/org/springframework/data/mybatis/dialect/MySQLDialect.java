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

import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.MySQLIdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;
import org.springframework.data.mybatis.repository.support.ResidentParameterName;

/**
 * MySQLDialect.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class MySQLDialect extends Dialect {

	private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {

			if (null != selection) {
				final boolean hasOffset = LimitHelper.hasFirstRow(selection);
				return sql + (hasOffset
						? String.format(" limit %d, %d", LimitHelper.getFirstRow(selection), selection.getMaxRows())
						: String.format(" limit %d", selection.getMaxRows()));
			}

			return sql + String.format(" limit #{%s},#{%s}", ResidentParameterName.OFFSET,
					ResidentParameterName.PAGE_SIZE);
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}
	};

	public MySQLDialect() {
		super();
	}

	@Override
	public char closeQuote() {
		return '`';
	}

	@Override
	public char openQuote() {
		return '`';
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new MySQLIdentityColumnSupport();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

}
