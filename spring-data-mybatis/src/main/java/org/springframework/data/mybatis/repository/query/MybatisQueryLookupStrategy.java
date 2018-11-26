package org.springframework.data.mybatis.repository.query;

import java.lang.reflect.Method;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

public final class MybatisQueryLookupStrategy {

	private MybatisQueryLookupStrategy() {
	}

	private abstract static class AbstractQueryLookupStrategy
			implements QueryLookupStrategy {

		private final SqlSessionTemplate sqlSessionTemplate;

		public AbstractQueryLookupStrategy(SqlSessionTemplate sqlSessionTemplate) {
			this.sqlSessionTemplate = sqlSessionTemplate;
		}

		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
				ProjectionFactory factory, NamedQueries namedQueries) {
			return resolveQuery(new MybatisQueryMethod(method, metadata, factory),
					sqlSessionTemplate, namedQueries);
		}

		protected abstract RepositoryQuery resolveQuery(MybatisQueryMethod method,
				SqlSessionTemplate sqlSessionTemplate, NamedQueries namedQueries);

	}

	private static class CreateQueryLookupStrategy extends AbstractQueryLookupStrategy {

		public CreateQueryLookupStrategy(SqlSessionTemplate sqlSessionTemplate) {
			super(sqlSessionTemplate);
		}

		@Override
		protected RepositoryQuery resolveQuery(MybatisQueryMethod method,
				SqlSessionTemplate sqlSessionTemplate, NamedQueries namedQueries) {
			return new PartTreeMybatisQuery(method, sqlSessionTemplate);
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
		protected RepositoryQuery resolveQuery(MybatisQueryMethod method,
				SqlSessionTemplate sqlSessionTemplate, NamedQueries namedQueries) {

			if (method.isAnnotatedQuery()) {
				return new SimpleMybatisQuery(method, sqlSessionTemplate);
			}

			throw new IllegalStateException(String.format(
					"Did neither find a NamedQuery nor an annotated query for method %s!",
					method));
		}

	}

	private static class CreateIfNotFoundQueryLookupStrategy
			extends AbstractQueryLookupStrategy {

		private final DeclaredQueryLookupStrategy lookupStrategy;

		private final CreateQueryLookupStrategy createStrategy;

		public CreateIfNotFoundQueryLookupStrategy(SqlSessionTemplate sqlSessionTemplate,

				CreateQueryLookupStrategy createStrategy,
				DeclaredQueryLookupStrategy lookupStrategy) {
			super(sqlSessionTemplate);
			this.lookupStrategy = lookupStrategy;
			this.createStrategy = createStrategy;
		}

		@Override
		protected RepositoryQuery resolveQuery(MybatisQueryMethod method,
				SqlSessionTemplate sqlSessionTemplate, NamedQueries namedQueries) {
			try {
				return lookupStrategy.resolveQuery(method, sqlSessionTemplate,
						namedQueries);
			}
			catch (IllegalStateException e) {
				return createStrategy.resolveQuery(method, sqlSessionTemplate,
						namedQueries);
			}
		}

	}

	public static QueryLookupStrategy create(SqlSessionTemplate sqlSessionTemplate,
			Key key, QueryMethodEvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		Assert.notNull(evaluationContextProvider,
				"EvaluationContextProvider must not be null!");

		switch (key != null ? key : Key.CREATE_IF_NOT_FOUND) {
		case CREATE:
			return new CreateQueryLookupStrategy(sqlSessionTemplate);
		case USE_DECLARED_QUERY:
			return new DeclaredQueryLookupStrategy(sqlSessionTemplate,
					evaluationContextProvider);
		case CREATE_IF_NOT_FOUND:
			return new CreateIfNotFoundQueryLookupStrategy(sqlSessionTemplate,
					new CreateQueryLookupStrategy(sqlSessionTemplate),
					new DeclaredQueryLookupStrategy(sqlSessionTemplate,
							evaluationContextProvider));
		default:
			throw new IllegalArgumentException(
					String.format("Unsupported query lookup strategy %s!", key));
		}
	}

}
