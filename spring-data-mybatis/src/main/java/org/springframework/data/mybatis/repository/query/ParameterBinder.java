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
package org.springframework.data.mybatis.repository.query;

import javax.persistence.Query;

import org.springframework.util.Assert;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class ParameterBinder {

	static final String PARAMETER_NEEDS_TO_BE_NAMED = "For queries with named parameters you need to use provide names for method parameters. Use @Param for query method parameters, or when on Java 8+ use the javac flag -parameters.";

	private final MybatisParameters parameters;

	private final Iterable<QueryParameterSetter> parameterSetters;

	private final boolean useJpaForPaging;

	ParameterBinder(MybatisParameters parameters, Iterable<QueryParameterSetter> parameterSetters) {
		this(parameters, parameterSetters, true);
	}

	public ParameterBinder(MybatisParameters parameters, Iterable<QueryParameterSetter> parameterSetters,
			boolean useJpaForPaging) {

		Assert.notNull(parameters, "MybatisParameters must not be null!");
		Assert.notNull(parameterSetters, "Parameter setters must not be null!");

		this.parameters = parameters;
		this.parameterSetters = parameterSetters;
		this.useJpaForPaging = useJpaForPaging;
	}

	public <T extends Query> T bind(T mybatisQuery, QueryParameterSetter.QueryMetadata metadata,
			MybatisParametersParameterAccessor accessor) {
		this.bind(metadata.withQuery(mybatisQuery), accessor, QueryParameterSetter.ErrorHandling.STRICT);
		return mybatisQuery;
	}

	public void bind(QueryParameterSetter.BindableQuery query, MybatisParametersParameterAccessor accessor,
			QueryParameterSetter.ErrorHandling errorHandling) {

		for (QueryParameterSetter setter : this.parameterSetters) {
			setter.setParameter(query, accessor, errorHandling);
		}
	}

	Query bindAndPrepare(Query query, QueryParameterSetter.QueryMetadata metadata,
			MybatisParametersParameterAccessor accessor) {

		this.bind(query, metadata, accessor);

		if (!this.useJpaForPaging || !this.parameters.hasPageableParameter() || accessor.getPageable().isUnpaged()) {
			return query;
		}

		query.setFirstResult((int) accessor.getPageable().getOffset());
		query.setMaxResults(accessor.getPageable().getPageSize());

		return query;
	}

}
