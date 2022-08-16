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

package io.easybest.mybatis.repository.query;

import io.easybest.mybatis.mapping.MybatisPersistentEntity;

import org.springframework.util.Assert;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 */
public class DefaultMybatisEntityMetadata<T> implements MybatisEntityMetadata<T> {

	private final MybatisPersistentEntity<T> entity;

	public DefaultMybatisEntityMetadata(MybatisPersistentEntity<T> entity) {

		Assert.notNull(entity, "entity must not be null!");
		this.entity = entity;

	}

	@Override
	public String getEntityName() {

		return this.entity.getEntityName();
	}

	@Override
	public String getTableName() {

		return this.entity.getTableName().getReference();
	}

	@Override
	public Class<T> getJavaType() {
		return this.entity.getType();
	}

}
