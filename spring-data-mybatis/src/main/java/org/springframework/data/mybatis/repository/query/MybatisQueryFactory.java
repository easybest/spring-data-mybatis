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

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Factory to create the appropriate
 * {@link org.springframework.data.repository.query.RepositoryQuery} for a
 * {@link MybatisQueryMethod}.
 *
 * @author JARVIS SONG
 */
@Slf4j
enum MybatisQueryFactory {

	INSTANCE;

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Nullable
	AbstractMybatisQuery createQuery(MybatisMappingContext mappingContext, SqlSessionTemplate sqlSessionTemplate,
			MybatisQueryMethod method, QueryMethodEvaluationContextProvider evaluationContextProvider,
			NamedQueries namedQueries) {

		// Annotated Query
		if (StringUtils.hasText(method.getAnnotatedQuery())) {
			return new SimpleMybatisQuery(sqlSessionTemplate, method, method.getRequiredAnnotatedQuery(),
					method.getAnnotatedCountQuery(), evaluationContextProvider, PARSER);
		}

		// Query in mapper
		if (method.hasQueryAnnotation()) {
			return new MybatisDirectlyQuery(sqlSessionTemplate, method);
		}

		// Procedure query
		if (method.isProcedureQuery()) {
			return new StoredProcedureMybatisQuery(sqlSessionTemplate, method);
		}

		// Named query from properties
		String name = method.getNamedQueryName();
		String countName = method.getNamedCountQueryName();
		if (namedQueries.hasQuery(name)) {
			return new SimpleMybatisQuery(sqlSessionTemplate, method, namedQueries.getQuery(name),
					(namedQueries.hasQuery(countName) ? namedQueries.getQuery(countName) : null),
					evaluationContextProvider, PARSER);
		}

		// Named query from JPA annotations
		String namedQuery = mappingContext.getNamedQuery(name);
		String namedCountQuery = mappingContext.getNamedQuery(countName);
		if (null != namedQuery) {
			return new SimpleMybatisQuery(sqlSessionTemplate, method, namedQuery, namedCountQuery,
					evaluationContextProvider, PARSER);
		}

		// MyBatis statement query
		Configuration configuration = sqlSessionTemplate.getConfiguration();
		if (configuration.hasStatement(method.getStatementId(), false)) {
			return new MybatisStatementQuery(sqlSessionTemplate, method);
		}

		return null;
	}

}
