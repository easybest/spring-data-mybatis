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

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * Base class for {@link String} based MyBatis queries.
 *
 * @author JARVIS SONG
 */
abstract class AbstractStringBasedMybatisQuery extends AbstractMybatisQuery {

	private final DeclaredQuery query;

	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	private final SpelExpressionParser parser;

	private final QueryParameterSetter.QueryMetadataCache metadataCache = new QueryParameterSetter.QueryMetadataCache();

	AbstractStringBasedMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method,
			String queryString, QueryMethodEvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser parser) {
		super(sqlSessionTemplate, method);

		Assert.hasText(queryString, "Query string must not be null or empty!");
		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null!");
		Assert.notNull(parser, "Parser must not be null!");

		this.evaluationContextProvider = evaluationContextProvider;
		this.parser = parser;

		this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation(), parser);

	}

	public DeclaredQuery getQuery() {
		return this.query;
	}

	// @Override
	protected Query doCreateQuery(MybatisParametersParameterAccessor accessor) {

		String sortedQueryString = QueryUtils.applySorting(this.query.getQueryString(), accessor.getSort(),
				this.query.getAlias());
		ResultProcessor processor = this.getQueryMethod().getResultProcessor().withDynamicProjection(accessor);

		Query query = this.createMybatisQuery(sortedQueryString, processor.getReturnedType());
		QueryParameterSetter.QueryMetadata metadata = this.metadataCache.getMetadata(sortedQueryString, query);

		// it is ok to reuse the binding contained in the ParameterBinder although we
		// create a new query String because the
		// parameters in the query do not change.
		return this.parameterBinder.get().bindAndPrepare(query, metadata, accessor);
	}

	protected Query createMybatisQuery(String queryString, ReturnedType returnedType) {

		return new MybatisQuery(this.getSqlSessionTemplate(), this.getQueryMethod().getStatementId());
	}

}
