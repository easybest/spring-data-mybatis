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

package io.easybest.mybatis.mapping.precompile;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisAssociation;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisStagingMapperSnippet extends MybatisMapperSnippet {

	private final EntityManager entityManager;

	private final MybatisAssociation association;

	public MybatisStagingMapperSnippet(EntityManager entityManager, MybatisAssociation association) {

		this.entityManager = entityManager;
		this.association = association;
	}

	public String getNamespace() {

		return null;
	}

	public SqlDefinition select() {

		if (!this.association.isToMany()) {
			return null;
		}

		return null;
	}

}
