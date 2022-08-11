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

import io.easybest.mybatis.mapping.precompile.Segment;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * A {@link Dialect} for MySQL.
 *
 * @author Jarvis Song
 */
public class MysqlDialect extends AbstractDialect {

	/**
	 * Singleton instance.
	 */
	public static final MysqlDialect INSTANCE = new MysqlDialect();

	private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

			final boolean hasOffset = null != offset;

			return sql + (hasOffset ? (" OFFSET " + offset + " LIMIT " + fetchSize) : (" LIMIT " + fetchSize));
		}
	};

	public MysqlDialect() {

		super();

	}

	@Override
	public String getIdentitySelectString() {
		return "select last_insert_id()";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return IDENTITY.name().toLowerCase();
	}

	@Override
	public String regexpLike(String column, String pattern) {
		return column + " REGEXP '" + pattern + "'";
	}

}
