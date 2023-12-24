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

/**
 * .
 *
 * @author Jarvis Song
 */
public class Ingres9Dialect extends IngresDialect {

	private static final PaginationHandler PAGINATION_HANDLER = new AbstractPaginationHandler() {
		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {
			final String soff = " offset " + offset;
			final String slim = " fetch first " + fetchSize + " rows only";
			final StringBuilder sb = new StringBuilder(sql.length() + soff.length() + slim.length()).append(sql);
			if (null != offset) {
				sb.append(soff);
			}
			if (null != fetchSize) {
				sb.append(slim);
			}
			return sb.toString();
		}
	};

	@Override
	public PaginationHandler getPaginationHandler() {
		return PAGINATION_HANDLER;
	}

	@Override
	public String getIdentitySelectString(String table, String column, int type) {
		return "select last_identity()";
	}

}
