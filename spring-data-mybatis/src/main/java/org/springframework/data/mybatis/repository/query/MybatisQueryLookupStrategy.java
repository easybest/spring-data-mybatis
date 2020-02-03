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

import java.lang.reflect.Method;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Query lookup strategy to execute finders.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public final class MybatisQueryLookupStrategy {

	private MybatisQueryLookupStrategy() {
	}

	public static QueryLookupStrategy create(SqlSessionTemplate sqlSessionTemplate,
			@Nullable QueryLookupStrategy.Key key, QueryMethodEvaluationContextProvider evaluationContextProvider,
			EscapeCharacter escape) {
		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");
		switch ((null != key) ? key : QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND) {

		case CREATE:
			return new CreateQueryLookupStrategy(sqlSessionTemplate, escape);
		case USE_DECLARED_QUERY:
			return new DeclaredQueryLookupStrategy(sqlSessionTemplate, evaluationContextProvider);
		case CREATE_IF_NOT_FOUND:
			return new CreateIfNotFoundQueryLookupStrategy(sqlSessionTemplate,
					new CreateQueryLookupStrategy(sqlSessionTemplate, escape),
					new DeclaredQueryLookupStrategy(sqlSessionTemplate, evaluationContextProvider));
		default:
			throw new IllegalArgumentException(String.format("Unsupported query lookup strategy %s!", key));
		}
	}

	private abstract static class AbstractQueryLookupStrategy implements QueryLookupStrategy {

		private final SqlSessionTemplate sqlSessionTemplate;

		protected AbstractQueryLookupStrategy(SqlSessionTemplate sqlSessionTemplate) {
			this.sqlSessionTemplate = sqlSessionTemplate;
		}

		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {
			return this.resolveQuery(new MybatisQueryMethod(method, metadata, factory), this.sqlSessionTemplate,
					namedQueries);
		}

		protected abstract RepositoryQuery resolveQuery(MybatisQueryMethod method,
				SqlSessionTemplate sqlSessionTemplate, NamedQueries namedQueries);

	}

	private static class CreateQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final EscapeCharacter escape;

		protected CreateQueryLookupStrategy(SqlSessionTemplate sqlSessionTemplate, EscapeCharacter escape) {
			super(sqlSessionTemplate);
			this.escape = escape;
		}

		@Override
		protected RepositoryQuery resolveQuery(MybatisQueryMethod method, SqlSessionTemplate sqlSessionTemplate,
				NamedQueries namedQueries) {

			return new PartTreeMybatisQuery(method, this.escape);
		}

	}

	private static class DeclaredQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final QueryMethodEvaluationContextProvider evaluationContextProvider;

		protected DeclaredQueryLookupStrategy(SqlSessionTemplate sqlSessionTemplate,
				QueryMethodEvaluationContextProvider evaluationContextProvider) {
			super(sqlSessionTemplate);
			this.evaluationContextProvider = evaluationContextProvider;
		}

		@Override
		protected RepositoryQuery resolveQuery(MybatisQueryMethod method, SqlSessionTemplate sqlSessionTemplate,
				NamedQueries namedQueries) {
			RepositoryQuery query = MybatisQueryFactory.INSTANCE.fromQueryAnnotation(method,
					this.evaluationContextProvider);
			if (null != query) {
				return query;
			}
			query = MybatisQueryFactory.INSTANCE.fromProcedureAnnotation(method);
			if (null != query) {
				return query;
			}

			String name = method.getNamedQueryName();
			if (namedQueries.hasQuery(name)) {
				return MybatisQueryFactory.INSTANCE.fromMethodWithQueryString(method, namedQueries.getQuery(name),
						this.evaluationContextProvider);
			}

			query = NamedQuery.lookupFrom(method);
			if (null != query) {
				return query;
			}

			throw new IllegalStateException(
					String.format("Did neither find a NamedQuery nor an annotated query for method %s!", method));
		}

	}

	private static class CreateIfNotFoundQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final DeclaredQueryLookupStrategy lookupStrategy;

		private final CreateQueryLookupStrategy createStrategy;

		protected CreateIfNotFoundQueryLookupStrategy(SqlSessionTemplate sqlSessionTemplate,
				CreateQueryLookupStrategy createStrategy, DeclaredQueryLookupStrategy lookupStrategy) {
			super(sqlSessionTemplate);
			this.createStrategy = createStrategy;
			this.lookupStrategy = lookupStrategy;
		}

		@Override
		protected RepositoryQuery resolveQuery(MybatisQueryMethod method, SqlSessionTemplate sqlSessionTemplate,
				NamedQueries namedQueries) {
			try {
				return this.lookupStrategy.resolveQuery(method, sqlSessionTemplate, namedQueries);
			}
			catch (IllegalStateException ex) {
				return this.createStrategy.resolveQuery(method, sqlSessionTemplate, namedQueries);
			}
		}

	}

}
