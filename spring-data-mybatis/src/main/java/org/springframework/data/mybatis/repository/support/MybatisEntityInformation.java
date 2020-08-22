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

import org.springframework.data.mybatis.repository.query.MybatisEntityMetadata;
import org.springframework.data.repository.core.EntityInformation;

/**
 * Extension of {@link EntityInformation} to capture additional MyBatis specific
 * information about entities.
 *
 * @param <T> entity type
 * @param <ID> entity id type
 * @author JARVIS SONG
 * @since 2.0.0
 */
public interface MybatisEntityInformation<T, ID> extends EntityInformation<T, ID>, MybatisEntityMetadata<T> {

	/**
	 * Returns {@literal true} if the entity has a composite id.
	 * @return result
	 */
	boolean hasCompositeId();

	void initVersion(T entity);

}
