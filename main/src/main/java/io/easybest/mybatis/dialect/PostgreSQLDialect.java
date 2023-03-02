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

package io.easybest.mybatis.dialect;

import io.easybest.mybatis.mapping.precompile.Segment;

import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * .
 *
 * @author Jarvis Song
 */
public class PostgreSQLDialect extends AbstractDialect {

	private static final AbstractPaginationHandler PAGINATION_HANDLER = new AbstractPaginationHandler() {

		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

			final boolean hasOffset = null != offset;

			return sql + (hasOffset ? (" LIMIT " + fetchSize + " OFFSET " + offset) : (" LIMIT " + fetchSize));

		}
	};

	@Override
	public PaginationHandler getPaginationHandler() {
		return PAGINATION_HANDLER;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {

		return SEQUENCE.name().toLowerCase();
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select currval('" + table + '_' + column + "_seq')";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "NEXTVAL ('" + sequenceName + "')";
	}

	@Override
	public String limitN(int n) {
		return "LIMIT " + n;
	}

	@Override
	public String regexpLike(String column, String pattern) {
		return column + " ~ " + pattern;
	}

}
