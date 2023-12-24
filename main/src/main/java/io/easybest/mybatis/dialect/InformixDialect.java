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
public class InformixDialect extends AbstractDialect {

	private static final AbstractPaginationHandler PAGINATION_HANDLER = new AbstractPaginationHandler() {

		@Override
		public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

			final boolean hasOffset = null != offset;

			StringBuilder builder = new StringBuilder();
			builder.append("SELECT ");
			if (hasOffset) {
				builder.append(" SKIP ").append(offsetEnd);
			}
			if (null != fetchSize) {
				builder.append(" FIRST ").append(fetchSize);
			}
			builder.append(" * FROM ( ");
			builder.append(sql);
			builder.append(" ) row_");

			return builder.toString();
		}
	};

	@Override
	public PaginationHandler getPaginationHandler() {

		return PAGINATION_HANDLER;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "SELECT " + sequenceName + ".NEXTVAL";
	}

	@Override
	public String regexpLike(String column, String pattern) {
		return "regex_match(" + column + "," + pattern + ")";
	}

}
