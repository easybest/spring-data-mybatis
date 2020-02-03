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

import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;

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
	AbstractMybatisQuery fromQueryAnnotation(MybatisQueryMethod method,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		log.debug("Looking up query for method {}", method.getName());
		return fromMethodWithQueryString(method, method.getAnnotatedQuery(), evaluationContextProvider);
	}

	@Nullable
	AbstractMybatisQuery fromMethodWithQueryString(MybatisQueryMethod method, @Nullable String queryString,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		if (queryString == null) {
			return null;
		}

		return new SimpleMybatisQuery(method, queryString, evaluationContextProvider, PARSER);
	}

	@Nullable
	public StoredProcedureMybatisQuery fromProcedureAnnotation(MybatisQueryMethod method) {

		if (!method.isProcedureQuery()) {
			return null;
		}

		return new StoredProcedureMybatisQuery(method);
	}

}
