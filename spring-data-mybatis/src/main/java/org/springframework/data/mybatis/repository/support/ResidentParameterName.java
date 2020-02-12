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
package org.springframework.data.mybatis.repository.support;

/**
 * Resident parameter names.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public final class ResidentParameterName {

	private ResidentParameterName() {
	}

	/**
	 * The prefix of parameter placeholder.
	 */
	private static final String PREFIX = "__";

	/**
	 * For <code>__sort</code>.
	 */
	public static final String SORT = PREFIX + "sort";

	/**
	 * For <code>__offset</code>.
	 */
	public static final String OFFSET = PREFIX + "offset";

	/**
	 * For <code>__pageSize</code>.
	 */
	public static final String PAGE_SIZE = PREFIX + "pageSize";

	/**
	 * For <code>__ids</code>.
	 */
	public static final String IDS = PREFIX + "ids";

	/**
	 * For <code>__condition</code>.
	 */
	public static final String CONDITION = PREFIX + "condition";

	/**
	 * For <code>__entity</code>.
	 */
	public static final String ENTITY = PREFIX + "entity";

	/**
	 * For <code>__id</code>.
	 */
	public static final String ID = PREFIX + "id";

	/**
	 * For <code>__p</code>.
	 */
	public static final String POSITION_PREFIX = PREFIX + "p";

	/**
	 * For <code>__example</code>.
	 */
	public static final String EXAMPLE = PREFIX + "example";

	/**
	 * For <code>__matcher</code>.
	 */
	public static final String MATCHER = PREFIX + "matcher";

	/**
	 * For <code>__accessor</code>.
	 */
	public static final String ACCESSOR = PREFIX + "accessor";

}
