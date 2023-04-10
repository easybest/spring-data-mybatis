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

package io.easybest.mybatis.compatibility;

import java.util.Optional;

import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;

/**
 * .
 *
 * @author Jarvis Song
 */
public interface PersistenceCompatibility {

	boolean isAnnotatedIdClass(PersistentEntity<?, ?> entity);

	boolean isAnnotatedEmbeddedId(PersistentProperty<?> property);

	String getAnnotatedTableName(PersistentEntity<?, ?> entity);

	String getAnnotatedEntityName(PersistentEntity<?, ?> entity);

	/**
	 * The specification of generation strategies for the values of primary keys.
	 * @param property property
	 * @return GenerationType and generator
	 */
	Optional<String[]> getAnnotatedGenerationType(PersistentProperty<?> property);

	Optional<Object[]> getAnnotatedOneToMany(PersistentProperty<?> property);

	Optional<Object[]> getAnnotatedManyToMany(PersistentProperty<?> property);

	Optional<Object[]> getAnnotatedManyToOne(PersistentProperty<?> property);

	Optional<Object[]> getAnnotatedOneToOne(PersistentProperty<?> property);

	Optional<Object[]> getAnnotatedElementCollection(PersistentProperty<?> property);

}
