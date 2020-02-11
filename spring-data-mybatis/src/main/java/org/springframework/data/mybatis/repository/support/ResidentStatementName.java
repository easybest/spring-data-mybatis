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
 * The resident statement names.
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public final class ResidentStatementName {

	private ResidentStatementName() {
	}

	/**
	 * Prefix of resident statement names.
	 */
	private static final String PREFIX = "__";

	/**
	 * For <code>__result_map</code>.
	 */
	public static final String RESULT_MAP = PREFIX + "result_map";

	/**
	 * For <code>__result_map_</code>.
	 */
	public static final String RESULT_MAP_PREFIX = PREFIX + "__result_map_";

	/**
	 * For <code>__get_by_id</code>.
	 */
	public static final String GET_BY_ID = PREFIX + "get_by_id";

	/**
	 * For <code>__find</code>.
	 */
	public static final String FIND = PREFIX + "find";

	/**
	 * For <code>__count_all</code>.
	 */
	public static final String COUNT_ALL = PREFIX + "count_all";

	/**
	 * For <code>__delete_by_id</code>.
	 */
	public static final String DELETE_BY_ID = PREFIX + "delete_by_id";

	/**
	 * For <code>__delete_all</code>.
	 */
	public static final String DELETE_ALL = PREFIX + "delete_all";

	/**
	 * For <code>__delete_by_ids</code>.
	 */
	public static final String DELETE_BY_IDS = PREFIX + "delete_by_ids";

	/**
	 * For <code>__delete_by_example</code>.
	 */
	public static final String DELETE_BY_EXAMPLE = PREFIX + "delete_by_example";

	/**
	 * For <code>__find_by_pager</code>.
	 */
	public static final String FIND_BY_PAGER = PREFIX + "find_by_pager";

	/**
	 * For <code>__find_by_example</code>.
	 */
	public static final String FIND_BY_EXAMPLE = PREFIX + "find_by_example";

	/**
	 * For <code>__count</code>.
	 */
	public static final String COUNT = PREFIX + "count";

	/**
	 * For <code>__count_by_example</code>.
	 */
	public static final String COUNT_BY_EXAMPLE = PREFIX + "count_by_example";

	/**
	 * For <code>__insert</code>.
	 */
	public static final String INSERT = PREFIX + "insert";

	/**
	 * For <code>__update</code>.
	 */
	public static final String UPDATE = PREFIX + "update";

	/**
	 * For <code>__insert_selective</code>.
	 */
	public static final String INSERT_SELECTIVE = INSERT + "_selective";

	/**
	 * For <code>__update_by_id</code>.
	 */
	public static final String UPDATE_BY_ID = UPDATE + "_by_id";

	/**
	 * For <code>__update_selective</code>.
	 */
	public static final String UPDATE_SELECTIVE = UPDATE + "_selective";

	/**
	 * For <code>__update_selective_by_id</code>.
	 */
	public static final String UPDATE_SELECTIVE_BY_ID = UPDATE_SELECTIVE + "_by_id";

	/**
	 * For <code>__sort</code>.
	 */
	public static final String SORT = "__sort";

	/**
	 * For <code>__offset</code>.
	 */
	public static final String OFFSET = "__offset";

	/**
	 * For <code>__pageSize</code>.
	 */
	public static final String PAGE_SIZE = "__pageSize";

	/**
	 * For <code>__count_</code>.
	 */
	public static final String COUNT_PREFIX = "__count_";

	/**
	 * For <code>__p</code>.
	 */
	public static final String PARAMETER_POSITION_PREFIX = "__p";

	/**
	 * For <code>__unpaged_</code>.
	 */
	public static final String UNPAGED_PREFIX = "__unpaged_";

	/**
	 * For <code>__query_</code>.
	 */
	public static final String QUERY_PREFIX = "__query_";

	public static String statementName(String namespace, String statement) {
		return namespace + '.' + statement;
	}

}
