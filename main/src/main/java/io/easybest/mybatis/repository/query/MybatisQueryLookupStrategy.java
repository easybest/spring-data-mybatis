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

import java.lang.reflect.Method;

import io.easybest.mybatis.mapping.EntityManager;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import static org.springframework.data.repository.query.QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;

/**
 * .
 *
 * @author Jarvis Song
 */
public final class MybatisQueryLookupStrategy {

	private MybatisQueryLookupStrategy() {
	}

	public static QueryLookupStrategy create(EntityManager entityManager, @Nullable QueryLookupStrategy.Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(entityManager, "EntityManager must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

		switch (key != null ? key : CREATE_IF_NOT_FOUND) {

		case CREATE:
			return new CreateQueryLookupStrategy(entityManager);
		case USE_DECLARED_QUERY:
			return new DeclaredQueryLookupStrategy(entityManager, evaluationContextProvider);
		case CREATE_IF_NOT_FOUND:
			return new CreateIfNotFoundQueryLookupStrategy(entityManager, new CreateQueryLookupStrategy(entityManager),
					new DeclaredQueryLookupStrategy(entityManager, evaluationContextProvider));
		default:
			throw new IllegalArgumentException(String.format("Unsupported query lookup strategy %s!", key));
		}

	}

	private static class CreateQueryLookupStrategy extends AbstractQueryLookupStrategy {

		protected CreateQueryLookupStrategy(EntityManager entityManager) {
			super(entityManager);
		}

		@Override
		protected RepositoryQuery resolveQuery(EntityManager entityManager, NamedQueries namedQueries,
				MybatisQueryMethod method) {

			return new PartTreeMybatisQuery(entityManager, method);
		}

	}

	private final static class DeclaredQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final QueryMethodEvaluationContextProvider evaluationContextProvider;

		private DeclaredQueryLookupStrategy(EntityManager entityManager,
				QueryMethodEvaluationContextProvider evaluationContextProvider) {

			super(entityManager);

			this.evaluationContextProvider = evaluationContextProvider;
		}

		@Override
		protected RepositoryQuery resolveQuery(EntityManager entityManager, NamedQueries namedQueries,
				MybatisQueryMethod method) {

			AbstractMybatisQuery query = MybatisQueryFactory.INSTANCE.create(entityManager, method,
					this.evaluationContextProvider, namedQueries);
			if (null != query) {
				return query;
			}

			throw new IllegalStateException(
					String.format("Did neither find a NamedQuery nor an annotated query for method %s!", method));
		}

	}

	private static class CreateIfNotFoundQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final CreateQueryLookupStrategy createQueryLookupStrategy;

		private final DeclaredQueryLookupStrategy declaredQueryLookupStrategy;

		public CreateIfNotFoundQueryLookupStrategy(EntityManager entityManager,
				CreateQueryLookupStrategy createQueryLookupStrategy,
				DeclaredQueryLookupStrategy declaredQueryLookupStrategy) {

			super(entityManager);

			Assert.notNull(createQueryLookupStrategy, "CreateQueryLookupStrategy must not be null!");
			Assert.notNull(declaredQueryLookupStrategy, "DeclaredQueryLookupStrategy must not be null!");

			this.createQueryLookupStrategy = createQueryLookupStrategy;
			this.declaredQueryLookupStrategy = declaredQueryLookupStrategy;
		}

		@Override
		protected RepositoryQuery resolveQuery(EntityManager entityManager, NamedQueries namedQueries,
				MybatisQueryMethod method) {

			try {
				return this.declaredQueryLookupStrategy.resolveQuery(entityManager, namedQueries, method);
			}
			catch (IllegalStateException ex) {
				return this.createQueryLookupStrategy.resolveQuery(entityManager, namedQueries, method);
			}

		}

	}

	private abstract static class AbstractQueryLookupStrategy implements QueryLookupStrategy {

		private final EntityManager entityManager;

		protected AbstractQueryLookupStrategy(EntityManager entityManager) {

			Assert.notNull(entityManager, "EntityManager must not be null!");

			this.entityManager = entityManager;
		}

		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {

			return this.resolveQuery(this.entityManager, namedQueries,
					new MybatisQueryMethod(this.entityManager, method, metadata, factory));
		}

		protected abstract RepositoryQuery resolveQuery(EntityManager entityManager, NamedQueries namedQueries,
				MybatisQueryMethod method);

	}

}
