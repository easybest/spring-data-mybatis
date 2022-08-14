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

package io.easybest.mybatis.repository.support;

/**
 * Resident statement names.
 *
 * @author Jarvis Song
 */
public interface ResidentStatementName {

	/**
	 * Dot.
	 */
	char DOT = '.';

	/**
	 * The prefix of parameter placeholder.
	 */
	String PREFIX = "__";

	/**
	 * For <code>__column_list</code>.
	 */
	String COLUMN_LIST = PREFIX + "column_list";

	/**
	 * For <code>__column_list_using_type</code>.
	 */
	String COLUMN_LIST_USING_TYPE = PREFIX + "column_list_using_type";

	/**
	 * For <code>__column_list_base</code>.
	 */
	String BASE_COLUMN_LIST = COLUMN_LIST + "_base";

	/**
	 * For <code>__column_list_basic</code>.
	 */
	String BASIC_COLUMN_LIST = COLUMN_LIST + "_basic";

	/**
	 * For <code>__from</code>.
	 */
	String FROM = PREFIX + "from";

	/**
	 * For <code>__from_base</code>.
	 */
	String BASE_FROM = FROM + "_base";

	/**
	 * For <code>__select</code>.
	 */
	String SELECT = PREFIX + "select";

	/**
	 * For <code>__by_id_where_condition</code>.
	 */
	String BY_ID_WHERE_CONDITION = PREFIX + "by_id_where_condition";

	/**
	 * For <code>__by_ids_where_condition</code>.
	 */
	String BY_IDS_WHERE_CONDITION = PREFIX + "by_ids_where_condition";

	/**
	 * For <code>__standard_sort</code>.
	 */
	String STANDARD_SORT = PREFIX + "standard_sort";

	/**
	 * For <code>__result_map</code>.
	 */
	String RESULT_MAP = PREFIX + "result_map";

	/**
	 * For <code>__result_map_base</code>.
	 */
	String BASE_RESULT_MAP = RESULT_MAP + "_base";

	/**
	 * For <code>__result_map_basic</code>.
	 */
	String BASIC_RESULT_MAP = RESULT_MAP + "_basic";

	/**
	 * For <code>__result_map_lazy</code>.
	 */
	String LAZY_RESULT_MAP = RESULT_MAP + "_lazy";

	/**
	 * For <code>__find</code>.
	 */
	String FIND = PREFIX + "find";

	/**
	 * For <code>__find_all</code>.
	 */
	String FIND_ALL = PREFIX + "find_all";

	/**
	 * For <code>__find_by_page</code>.
	 */
	String FIND_BY_PAGE = PREFIX + "find_by_page";

	/**
	 * For <code>__count</code>.
	 */
	String COUNT = PREFIX + "count";

	/**
	 * For <code>__count_all</code>.
	 */
	String COUNT_ALL = PREFIX + "count_all";

	/**
	 * For <code>__delete_by_id</code>.
	 */
	String DELETE_BY_ID = PREFIX + "delete_by_id";

	/**
	 * For <code>__delete_by_ids</code>.
	 */
	String DELETE_BY_IDS = PREFIX + "delete_by_ids";

	/**
	 * For <code>__delete_by_entity</code>.
	 */
	String DELETE_BY_ENTITY = PREFIX + "delete_by_entity";

	/**
	 * For <code>__delete_by_entities</code>.
	 */
	String DELETE_BY_ENTITIES = PREFIX + "delete_by_entities";

	/**
	 * For <code>__delete_all</code>.
	 */
	String DELETE_ALL = PREFIX + "delete_all";

	/**
	 * For <code>__get_by_id</code>.
	 */
	String GET_BY_ID = PREFIX + "get_by_id";

	/**
	 * For <code>__find_by_id</code>.
	 */
	String FIND_BY_ID = PREFIX + "find_by_id";

	/**
	 * For <code>__find_by_ids</code>.
	 */
	String FIND_BY_IDS = PREFIX + "find_by_ids";

	/**
	 * For <code>__insert</code>.
	 */
	String INSERT = PREFIX + "insert";

	/**
	 * For <code>__update</code>.
	 */
	String UPDATE = PREFIX + "update";

	/**
	 * For <code>__insert_selective</code>.
	 */
	String INSERT_SELECTIVE = INSERT + "_selective";

	/**
	 * For <code>__update_selective</code>.
	 */
	String UPDATE_SELECTIVE = UPDATE + "_selective";

	/**
	 * For <code>__update_by_id</code>.
	 */
	String UPDATE_BY_ID = UPDATE + "_by_id";

	/**
	 * For <code>__update_selective_by_id</code>.
	 */
	String UPDATE_SELECTIVE_BY_ID = UPDATE_SELECTIVE + "_by_id";

	/**
	 * For <code>__count_</code>.
	 */
	String COUNT_PREFIX = "__count_";

	/**
	 * For <code>__unpaged_</code>.
	 */
	String UNPAGED_PREFIX = "__unpaged_";

	/**
	 * For <code>__query_</code>.
	 */
	String QUERY_PREFIX = "__query_";

	/**
	 * For <code>__query_by_example</code>.
	 */
	String QUERY_BY_EXAMPLE = "__query_by_example";

	/**
	 * For <code>__query_by_example_for_page</code>.
	 */
	String QUERY_BY_EXAMPLE_FOR_PAGE = QUERY_BY_EXAMPLE + "_for_page";

	/**
	 * For <code>__count_query_by_example</code>.
	 */
	String COUNT_QUERY_BY_EXAMPLE = "__count_query_by_example";

	/**
	 * For <code>__exists_by_example</code>.
	 */
	String EXISTS_BY_EXAMPLE = "__exists_by_example";

	/**
	 * For <code>__exists_by_id</code>.
	 */
	String EXISTS_BY_ID = "__exists_by_id";

	/**
	 * For <code>__table_name</code>.
	 */
	String TABLE_NAME = "__table_name";

	/**
	 * For <code>__table_name_pure</code>.
	 */
	String TABLE_NAME_PURE = "__table_name_pure";

	default String statementName(String namespace, String statement) {
		return namespace + DOT + statement;
	}

}
