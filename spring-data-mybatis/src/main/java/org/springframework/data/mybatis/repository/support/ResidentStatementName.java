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
	 * For <code>__basic_result_map</code>.
	 */
	public static final String BASIC_RESULT_MAP = PREFIX + "basic_result_map";

	/**
	 * For <code>__result_map_</code>.
	 */
	public static final String RESULT_MAP_PREFIX = PREFIX + "result_map_";

	/**
	 * For <code>__basic_column_list</code>.
	 */
	public static final String BASIC_COLUMN_LIST = PREFIX + "basic_column_list";

	/**
	 * For <code>__from</code>.
	 */
	public static final String FROM = PREFIX + "from";

	/**
	 * For <code>__select</code>.
	 */
	public static final String SELECT = PREFIX + "select";

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
	 * For <code>__delete</code>.
	 */
	public static final String DELETE = PREFIX + "delete";

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
	 * For <code>__count_</code>.
	 */
	public static final String COUNT_PREFIX = "__count_";

	/**
	 * For <code>__unpaged_</code>.
	 */
	public static final String UNPAGED_PREFIX = "__unpaged_";

	/**
	 * For <code>__query_</code>.
	 */
	public static final String QUERY_PREFIX = "__query_";

	/**
	 * For <code>__query_by_example</code>.
	 */
	public static final String QUERY_BY_EXAMPLE = "__query_by_example";

	/**
	 * For <code>__count_query_by_example</code>.
	 */
	public static final String COUNT_QUERY_BY_EXAMPLE = "__count_query_by_example";

	/**
	 * For <code>__query_by_example_where_clause</code>.
	 */
	public static final String QUERY_BY_EXAMPLE_WHERE_CLAUSE = QUERY_BY_EXAMPLE + "_where_clause";

	/**
	 * For <code>__query_by_example_for_page</code>.
	 */
	public static final String QUERY_BY_EXAMPLE_FOR_PAGE = QUERY_BY_EXAMPLE + "_for_page";

	/**
	 * For <code>__standard_sort</code>.
	 */
	public static final String STANDARD_SORT = PREFIX + "standard_sort";

	/**
	 * For <code>__where_clause_by_id</code>.
	 */
	public static final String WHERE_BY_ID_CLAUSE = PREFIX + "where_clause_by_id";

	/**
	 * For <code>__where_clause_by_ids</code>.
	 */
	public static final String WHERE_BY_IDS_CLAUSE = PREFIX + "where_clause_by_ids";

	/**
	 * For <code>__where_clause_by_fixed_id</code>.
	 */
	public static final String WHERE_BY_FIXED_ID_CLAUSE = PREFIX + "where_clause_by_fixed_id";

	/**
	 * For <code>__where_clause_by_entity</code>.
	 */
	public static final String WHERE_BY_ENTITY_CLAUSE = PREFIX + "where_clause_by_entity";

	/**
	 * For <code>__base_column_list</code>.
	 */
	public static final String BASE_COLUMN_LIST = PREFIX + "base_column_list";

	/**
	 * For <code>__all_column_list</code>.
	 */
	public static final String ALL_COLUMN_LIST = PREFIX + "all_column_list";

	/**
	 * For <code>__column_list</code>.
	 */
	public static final String COLUMN_LIST = PREFIX + "column_list";

	public static String statementName(String namespace, String statement) {
		return namespace + '.' + statement;
	}

}
