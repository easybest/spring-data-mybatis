package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * @author Jarvis Song
 */
public final class MyBatisQueryLookupStrategy {

	/**
	 * Private constructor to prevent instantiation.
	 */
	private MyBatisQueryLookupStrategy() {}

	private abstract static class AbstractQueryLookupStrategy implements QueryLookupStrategy {
		private final SqlSessionTemplate template;

		public AbstractQueryLookupStrategy(SqlSessionTemplate template) {
			this.template = template;
		}

		@Override
		public final RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {

			return resolveQuery(new MybatisQueryMethod(method, metadata, factory), template, namedQueries);
		}

		protected abstract RepositoryQuery resolveQuery(MybatisQueryMethod method, SqlSessionTemplate template,
				NamedQueries namedQueries);
	}

	private static class CreateQueryLookupStrategy extends AbstractQueryLookupStrategy {

		public CreateQueryLookupStrategy(SqlSessionTemplate template) {
			super(template);
		}

		@Override
		protected RepositoryQuery resolveQuery(MybatisQueryMethod method, SqlSessionTemplate template,
				NamedQueries namedQueries) {
			return new PartTreeMyBatisQuery(method, template);
		}
	}

	private static class DeclaredQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final EvaluationContextProvider evaluationContextProvider;

		public DeclaredQueryLookupStrategy(SqlSessionTemplate template,
				EvaluationContextProvider evaluationContextProvider) {
			super(template);
			this.evaluationContextProvider = evaluationContextProvider;
		}

		@Override
		protected RepositoryQuery resolveQuery(MybatisQueryMethod method, SqlSessionTemplate template,
				NamedQueries namedQueries) {
			// TODO
			return null;
		}
	}

	private static class CreateIfNotFoundQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final DeclaredQueryLookupStrategy lookupStrategy;
		private final CreateQueryLookupStrategy createStrategy;

		public CreateIfNotFoundQueryLookupStrategy(SqlSessionTemplate template, CreateQueryLookupStrategy createStrategy,
				DeclaredQueryLookupStrategy lookupStrategy) {
			super(template);
			this.lookupStrategy = lookupStrategy;
			this.createStrategy = createStrategy;
		}

		@Override
		protected RepositoryQuery resolveQuery(MybatisQueryMethod method, SqlSessionTemplate template,
				NamedQueries namedQueries) {
			try {
				return lookupStrategy.resolveQuery(method, template, namedQueries);
			} catch (IllegalStateException e) {
				return createStrategy.resolveQuery(method, template, namedQueries);
			}
		}
	}

	public static QueryLookupStrategy create(SqlSessionTemplate sqlSessionTemplate, @Nullable QueryLookupStrategy.Key key,
			EvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

		switch (key != null ? key : QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND) {
			case CREATE:
				return new CreateQueryLookupStrategy(sqlSessionTemplate);
			case USE_DECLARED_QUERY:
				return new DeclaredQueryLookupStrategy(sqlSessionTemplate, evaluationContextProvider);
			case CREATE_IF_NOT_FOUND:
				return new CreateIfNotFoundQueryLookupStrategy(sqlSessionTemplate,
						new CreateQueryLookupStrategy(sqlSessionTemplate),
						new DeclaredQueryLookupStrategy(sqlSessionTemplate, evaluationContextProvider));
			default:
				throw new IllegalArgumentException(String.format("Unsupported query lookup strategy %s!", key));
		}
	}
}
