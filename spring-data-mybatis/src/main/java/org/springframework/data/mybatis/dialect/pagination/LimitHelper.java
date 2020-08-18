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
package org.springframework.data.mybatis.dialect.pagination;

/**
 * A helper for dealing with LimitHandler implementations.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public final class LimitHelper {

	private LimitHelper() {
	}

	public static boolean hasMaxRows(RowSelection selection) {
		return selection != null && selection.getMaxRows() != null && !"0".equals(selection.getMaxRows());
	}

	public static boolean useLimit(LimitHandler limitHandler, RowSelection selection) {
		return limitHandler.supportsLimit() && hasMaxRows(selection);
	}

	public static boolean hasFirstRow(RowSelection selection) {
		String firstRow = getFirstRow(selection);
		if (null == firstRow || "0".equals(firstRow)) {
			return false;
		}
		return true;
	}

	public static String getFirstRow(RowSelection selection) {
		return (selection == null || selection.getFirstRow() == null) ? String.valueOf(0) : selection.getFirstRow();
	}

}
