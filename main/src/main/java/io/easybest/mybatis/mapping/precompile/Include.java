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

package io.easybest.mybatis.mapping.precompile;

import io.easybest.mybatis.repository.support.ResidentStatementName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * .
 *
 * @author Jarvis Song
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Include implements Segment {

	/**
	 * Table name.
	 */
	public static Include TABLE_NAME = Include.of(ResidentStatementName.TABLE_NAME);

	/**
	 * Pure table name.
	 */
	public static Include TABLE_NAME_PURE = Include.of(ResidentStatementName.TABLE_NAME_PURE);

	/**
	 * Column list.
	 */
	public static Include COLUMN_LIST = Include.of(ResidentStatementName.COLUMN_LIST);

	/**
	 * Column list.
	 */
	public static Include COLUMN_LIST_PURE = Include.of(ResidentStatementName.COLUMN_LIST_PURE);

	/**
	 * COLUMN_LIST_USING_TYPE.
	 */
	public static Include COLUMN_LIST_USING_TYPE = Include.of(ResidentStatementName.COLUMN_LIST_USING_TYPE);

	private String id;

	public static Include of(String id) {
		return new Include(id);
	}

	@Override
	public String toString() {
		return "<include refid=\"" + this.getId() + "\"/>";
	}

}
