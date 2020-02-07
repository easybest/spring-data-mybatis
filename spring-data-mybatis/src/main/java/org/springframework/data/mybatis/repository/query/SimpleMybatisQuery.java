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
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * {@link RepositoryQuery} implementation that inspects a
 * {@link org.springframework.data.repository.query.QueryMethod} for the existence of an
 * {@link org.springframework.data.mybatis.repository.Query} annotation and creates a
 * MyBatis {@link Query} from it.
 *
 * @author JARVIS SONG
 */
final class SimpleMybatisQuery extends AbstractStringBasedMybatisQuery {

	SimpleMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method, String queryString,
			QueryMethodEvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {
		super(sqlSessionTemplate, method, queryString, evaluationContextProvider, parser);
	}

	SimpleMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method,
			QueryMethodEvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {
		super(sqlSessionTemplate, method, method.getRequiredAnnotatedQuery(), evaluationContextProvider, parser);
	}

}
