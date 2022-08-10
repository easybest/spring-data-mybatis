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

import io.easybest.mybatis.dialect.Dialect;
import io.easybest.mybatis.repository.query.EscapeCharacter;
import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

/**
 * .
 *
 * @author Jarvis Song
 */
public interface EntityManager extends MappingContext<MybatisPersistentEntityImpl<?>, MybatisPersistentPropertyImpl> {

	SqlSessionTemplate getSqlSessionTemplate();

	Dialect getDialect();

	@Nullable
	String getNamedQuery(String name);

	EscapeCharacter getEscapeCharacter();

}
