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

import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.support.QueryCreationListener;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.NonNull;

import io.easybest.mybatis.mapping.precompile.MybatisAggregateRootPrecompile;

/**
 * .
 *
 * @author Jarvis Song
 */
public enum MybatisQueryIntendListener implements QueryCreationListener<RepositoryQuery> {

	/**
	 * Singleton instance.
	 */
	INSTANCE;

	@Override
	public void onCreation(@NonNull RepositoryQuery query) {

		if (query instanceof IndicatingMybatisQuery) {
			MybatisAggregateRootPrecompile.compile((IndicatingMybatisQuery) query);
			return;
		}

		if (query instanceof PartTreeMybatisQuery) {
			MybatisAggregateRootPrecompile.compile((PartTreeMybatisQuery) query);
			return;
		}

		if (query instanceof MapperedMybatisQuery) {
			// don't have to do anything
			return;
		}

		throw new MappingException("Unsupported query " + query.getClass() + ", " + query.getQueryMethod());
	}

}
