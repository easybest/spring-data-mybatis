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
 * Resident parameter names.
 *
 * @author Jarvis Song
 * @since 2.0.0
 */
public interface ResidentParameterName {

	/**
	 * The prefix of parameter placeholder.
	 */
	String PREFIX = "__";

	/**
	 * For <code>__sort</code>.
	 */
	String SORT = PREFIX + "sort";

	/**
	 * For <code>__offset</code>.
	 */
	String OFFSET = PREFIX + "offset";

	/**
	 * For <code>__End</code>.
	 */
	String OFFSET_END = OFFSET + "End";

	/**
	 * For <code>__pageSize</code>.
	 */
	String PAGE_SIZE = PREFIX + "pageSize";

	/**
	 * For <code>__ids</code>.
	 */
	String IDS = PREFIX + "ids";

	/**
	 * For <code>__entity</code>.
	 */
	String ENTITY = PREFIX + "entity";

	/**
	 * For <code>__probe</code>.
	 */
	String PROBE = PREFIX + "probe";

	/**
	 * For <code>__persistentEntity</code>.
	 */
	String PERSISTENT_ENTITY = PREFIX + "persistentEntity";

	/**
	 * For <code>__entityManager</code>.
	 */
	String ENTITY_MANAGER = PREFIX + "entityManager";

	/**
	 * For <code>__id</code>.
	 */
	String ID = PREFIX + "id";

	/**
	 * For <code>__entities</code>.
	 */
	String ENTITIES = PREFIX + "entities";

	/**
	 * For <code>__p</code>.
	 */
	String POSITION_PREFIX = PREFIX + "p";

	/**
	 * For <code>__example</code>.
	 */
	String EXAMPLE = PREFIX + "example";

	/**
	 * For <code>__matcher</code>.
	 */
	String MATCHER = PREFIX + "matcher";

	/**
	 * For <code>__accessor</code>.
	 */
	String ACCESSOR = PREFIX + "accessor";

}
