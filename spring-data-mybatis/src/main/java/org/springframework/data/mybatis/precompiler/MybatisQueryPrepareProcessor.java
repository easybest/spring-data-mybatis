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
package org.springframework.data.mybatis.precompiler;

import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.query.PartTreeMybatisQuery;
import org.springframework.data.mybatis.repository.query.SimpleMybatisQuery;
import org.springframework.data.repository.core.support.QueryCreationListener;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Prepare mybatis mapper statement when a query created.
 *
 * @author JARVIS SONG
 * @since 2.0.1
 */
public class MybatisQueryPrepareProcessor implements QueryCreationListener<RepositoryQuery> {

	private final MybatisMappingContext mappingContext;

	public MybatisQueryPrepareProcessor(MybatisMappingContext mappingContext) {

		this.mappingContext = mappingContext;
	}

	@Override
	public void onCreation(RepositoryQuery query) {

		if (query instanceof SimpleMybatisQuery) {
			new SimpleMybatisQueryPrecompiler(this.mappingContext, (SimpleMybatisQuery) query).compile();
			return;
		}

		if (query instanceof PartTreeMybatisQuery) {
			new PartTreeMybatisQueryPrecompiler(this.mappingContext, (PartTreeMybatisQuery) query).compile();
			return;
		}
	}

}
