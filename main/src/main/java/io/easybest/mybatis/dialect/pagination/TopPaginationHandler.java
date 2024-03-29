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

package io.easybest.mybatis.dialect.pagination;

import java.util.Locale;

import io.easybest.mybatis.dialect.AbstractPaginationHandler;
import io.easybest.mybatis.mapping.precompile.Segment;

/**
 * .
 *
 * @author Jarvis Song
 */
public class TopPaginationHandler extends AbstractPaginationHandler {

	@Override
	public String processSql(String sql, Segment offset, Segment fetchSize, Segment offsetEnd) {

		if (null != offset) {
			throw new UnsupportedOperationException("Offset is not supported.");
		}

		final int selectIndex = sql.toLowerCase(Locale.ROOT).indexOf("select");
		final int selectDistinctIndex = sql.toLowerCase(Locale.ROOT).indexOf("select distinct");
		final int insertionPoint = selectIndex + (selectDistinctIndex == selectIndex ? 15 : 6);

		StringBuilder sb = new StringBuilder(sql.length() + 8).append(sql);

		sb.insert(insertionPoint, " TOP " + fetchSize + " ");

		return sb.toString();
	}

}
