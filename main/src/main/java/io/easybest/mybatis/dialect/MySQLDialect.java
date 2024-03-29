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
import io.easybest.mybatis.mapping.sql.IdentifierProcessing;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * A {@link Dialect} for MySQL.
 *
 * @author Jarvis Song
 */
public class MySQLDialect extends AbstractDialect {

	private static final IdentifierProcessing MYSQL_IDENTIFIER_PROCESSING = IdentifierProcessing
			.create(new IdentifierProcessing.Quoting("`"), IdentifierProcessing.LetterCasing.LOWER_CASE);

	private static final PaginationHandler PAGINATION_HANDLER = new AbstractPaginationHandler() {

		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

			final boolean hasOffset = null != offset;

			return sql + (hasOffset ? (" LIMIT " + offset + "," + fetchSize) : (" LIMIT " + fetchSize));
		}
	};

	public MySQLDialect() {

		super();
	}

	@Override
	public IdentifierProcessing getIdentifierProcessing() {
		return MYSQL_IDENTIFIER_PROCESSING;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_insert_id()";
	}

	@Override
	public PaginationHandler getPaginationHandler() {
		return PAGINATION_HANDLER;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return IDENTITY.name().toLowerCase();
	}

	@Override
	public String regexpLike(String column, String pattern) {
		return column + " REGEXP " + pattern;
	}

	@Override
	public String limitN(int n) {
		return "LIMIT " + n;
	}

}
