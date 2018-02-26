package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
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

		@Override
		public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {
			return resolveQuery(new MyBatisQueryMethod(method, metadata, factory), namedQueries);
		}

		/**
		 * * Resolves a {@link RepositoryQuery} from the given {@link MyBatisQueryMethod} that can be executed afterwards.
		 * 
		 * @param method
		 * @param namedQueries
		 * @return
		 */
		protected abstract RepositoryQuery resolveQuery(MyBatisQueryMethod method, NamedQueries namedQueries);

	}

	private static class CreateQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final SqlSessionTemplate template;

		private CreateQueryLookupStrategy(SqlSessionTemplate template) {
			this.template = template;
		}

		@Override
		protected RepositoryQuery resolveQuery(MyBatisQueryMethod method, NamedQueries namedQueries) {
			return new PartTreeMyBatisQuery(template, method);
		}
	}

	private static class DeclaredQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final SqlSessionTemplate template;
		private final EvaluationContextProvider evaluationContextProvider;

		private DeclaredQueryLookupStrategy(SqlSessionTemplate template,
				EvaluationContextProvider evaluationContextProvider) {
			this.template = template;
			this.evaluationContextProvider = evaluationContextProvider;
		}

		@Override
		protected RepositoryQuery resolveQuery(MyBatisQueryMethod method, NamedQueries namedQueries) {

			// TODO

			throw new IllegalStateException(
					String.format("Did neither find a NamedQuery nor an annotated query for method %s!", method));
		}
	}

	private static class CreateIfNotFoundQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final DeclaredQueryLookupStrategy lookupStrategy;
		private final CreateQueryLookupStrategy createStrategy;

		private CreateIfNotFoundQueryLookupStrategy(DeclaredQueryLookupStrategy lookupStrategy,
				CreateQueryLookupStrategy createStrategy) {

			this.lookupStrategy = lookupStrategy;
			this.createStrategy = createStrategy;
		}

		@Override
		protected RepositoryQuery resolveQuery(MyBatisQueryMethod method, NamedQueries namedQueries) {
			try {
				return lookupStrategy.resolveQuery(method, namedQueries);
			} catch (IllegalStateException e) {
				return createStrategy.resolveQuery(method, namedQueries);
			}
		}

	}

	public static QueryLookupStrategy create(SqlSessionTemplate sqlSessionTemplate, MyBatisMappingContext context,
			@Nullable QueryLookupStrategy.Key key, EvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

		switch (key != null ? key : QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND) {
			case CREATE:
				return new CreateQueryLookupStrategy(sqlSessionTemplate);
			case USE_DECLARED_QUERY:
				return new DeclaredQueryLookupStrategy(sqlSessionTemplate, evaluationContextProvider);
			case CREATE_IF_NOT_FOUND:
				return new CreateIfNotFoundQueryLookupStrategy(
						new DeclaredQueryLookupStrategy(sqlSessionTemplate, evaluationContextProvider),
						new CreateQueryLookupStrategy(sqlSessionTemplate));
			default:
				throw new IllegalArgumentException(String.format("Unsupported query lookup strategy %s!", key));
		}
	}
}
