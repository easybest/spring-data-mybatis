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

package io.easybest.mybatis.repository.query;

import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import io.easybest.mybatis.mapping.EntityManager;

/**
 * .
 *
 * @author Jarvis Song
 */
public enum MybatisQueryFactory {

	/**
	 * Instance.
	 */
	INSTANCE;

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Nullable
	AbstractMybatisQuery create(EntityManager entityManager, MybatisQueryMethod method,
			QueryMethodEvaluationContextProvider evaluationContextProvider, NamedQueries namedQueries) {

		// Annotated query
		if (StringUtils.hasText(method.getAnnotatedQuery())) {

			return new IndicatingMybatisQuery(entityManager, PARSER, evaluationContextProvider, method,
					method.getRequiredAnnotatedQuery(), method.getCountQuery());
		}

		// procedure query
		if (method.isProcedureQuery()) {
			// TODO
			return null;
		}

		// Named query from properties
		String name = method.getNamedQueryName();
		String countName = method.getNamedCountQueryName();

		if (namedQueries.hasQuery(name)) {
			return new IndicatingMybatisQuery(entityManager, PARSER, evaluationContextProvider, method,
					namedQueries.getQuery(name),
					(namedQueries.hasQuery(countName) ? namedQueries.getQuery(countName) : null));
		}

		// Named query from JPA annotations
		String namedQuery = entityManager.getNamedQuery(name);
		if (StringUtils.hasText(namedQuery)) {
			return new IndicatingMybatisQuery(entityManager, PARSER, evaluationContextProvider, method, namedQuery,
					entityManager.getNamedQuery(countName));
		}

		// query in mapper xml
		if (method.hasQueryAnnotation()) {
			return new MapperedMybatisQuery(entityManager, method);
		}

		// Mybatis statement query
		if (null != entityManager.getSqlSessionTemplate() && entityManager.getSqlSessionTemplate().getConfiguration()
				.hasStatement(method.getStatementId(), false)) {
			return new MapperedMybatisQuery(entityManager, method);
		}

		return null;
	}

}
