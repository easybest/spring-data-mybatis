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

import static javax.persistence.GenerationType.SEQUENCE;

/**
 * A {@link Dialect} for MySQL.
 *
 * @author Jarvis Song
 */
public class H2Dialect extends AbstractDialect {

	/**
	 * Singleton instance.
	 */
	public static final H2Dialect INSTANCE = new H2Dialect();

	private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

			final boolean hasOffset = null != offset;

			return sql + (hasOffset ? (" LIMIT " + fetchSize + " OFFSET " + offset) : (" LIMIT " + fetchSize));
		}
	};

	public H2Dialect() {

		super();

	}

	@Override
	public String getIdentitySelectString() {
		return "CALL IDENTITY()";
	}

	@Override
	public String getIdentityInsertString() {
		return "null";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "call next value for " + sequenceName;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return SEQUENCE.name().toLowerCase();
	}

	@Override
	public String regexpLike(String column, String pattern) {
		return "REGEXP_LIKE(" + column + "," + pattern + ")";
	}

	@Override
	public String limitN(int n) {
		return "LIMIT " + n;
	}

}
