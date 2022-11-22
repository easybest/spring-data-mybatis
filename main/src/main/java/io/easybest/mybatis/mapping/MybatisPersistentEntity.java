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

package io.easybest.mybatis.mapping;

import java.util.Optional;

import org.springframework.data.mapping.model.MutablePersistentEntity;

import io.easybest.mybatis.mapping.sql.SqlIdentifier;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> entity type
 */
public interface MybatisPersistentEntity<T> extends MutablePersistentEntity<T, MybatisPersistentPropertyImpl> {

	SqlIdentifier getTableName();

	String getEntityName();

	boolean isCompositeId();

	Optional<String> getLogicDeleteColumn();

	/**
	 * Check whether the entity has associated attributes.
	 * @return result
	 */
	boolean isBasic();

}
