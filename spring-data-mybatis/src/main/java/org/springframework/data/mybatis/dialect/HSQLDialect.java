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

import org.springframework.data.mybatis.dialect.identity.HSQLIdentityColumnSupport;
import org.springframework.data.mybatis.dialect.identity.IdentityColumnSupport;
import org.springframework.data.mybatis.dialect.pagination.AbstractLimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.LimitHelper;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;

/**
 * HSQLDialect.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class HSQLDialect extends Dialect {

	private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {

		@Override
		public String processSql(String sql, RowSelection selection) {

			final boolean hasOffset = LimitHelper.hasFirstRow(selection);
			if (hasOffset) {
				return sql + " OFFSET " + LimitHelper.getFirstRow(selection) + " LIMIT " + selection.getMaxRows();
			}
			return sql + " LIMIT " + selection.getMaxRows();
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

	};

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "call next value for " + sequenceName;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new HSQLIdentityColumnSupport();
	}

	@Override
	public String getRegexLikeFunction(String column, String parameter) {
		return "REGEXP_MATCHES(" + column + ",'" + parameter + "')";
	}

}
