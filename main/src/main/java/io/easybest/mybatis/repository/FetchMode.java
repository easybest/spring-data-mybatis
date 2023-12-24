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

package io.easybest.mybatis.repository;

/**
 * Fetch options on associations.
 *
 * @author Jarvis Song
 */
public enum FetchMode {

	/**
	 * Use a secondary select for each individual entity, collection, or join load.
	 */
	SELECT,
	/**
	 * Use an outer join to load the related entities, collections or joins.
	 */
	JOIN,
	/**
	 * Available for collections only. When accessing a non-initialized collection, this
	 * fetch mode will trigger loading all elements of all collections of the same role
	 * for all owners associated with the persistence context using a single secondary
	 * select.
	 */
	SUBSELECT

}
