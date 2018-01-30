package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.repository.support.MyBatisMapperBuilderAssistant;
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
		private final MyBatisMapperBuilderAssistant mapperBuilderAssistant;
		private final MyBatisMappingContext context;
		private final SqlSessionTemplate template;
		private final Dialect dialect;

		public AbstractQueryLookupStrategy(MyBatisMapperBuilderAssistant mapperBuilderAssistant,
				MyBatisMappingContext context, SqlSessionTemplate template, Dialect dialect) {
			this.mapperBuilderAssistant = mapperBuilderAssistant;
			this.context = context;
			this.template = template;
			this.dialect = dialect;
		}

		@Override
		public final RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				NamedQueries namedQueries) {

			return resolveQuery(new MyBatisQueryMethod(method, metadata, factory), mapperBuilderAssistant, context, template,
					dialect, namedQueries);
		}

		protected abstract RepositoryQuery resolveQuery(MyBatisQueryMethod method,
				MyBatisMapperBuilderAssistant mapperBuilderAssistant, MyBatisMappingContext context,
				SqlSessionTemplate template, Dialect dialect, NamedQueries namedQueries);
	}

	private static class CreateQueryLookupStrategy extends AbstractQueryLookupStrategy {

		public CreateQueryLookupStrategy(MyBatisMapperBuilderAssistant mapperBuilderAssistant,
				MyBatisMappingContext context, SqlSessionTemplate template, Dialect dialect) {
			super(mapperBuilderAssistant, context, template, dialect);
		}

		@Override
		protected RepositoryQuery resolveQuery(MyBatisQueryMethod method,
				MyBatisMapperBuilderAssistant mapperBuilderAssistant, MyBatisMappingContext context,
				SqlSessionTemplate template, Dialect dialect, NamedQueries namedQueries) {
			return new PartTreeMyBatisQuery(method, mapperBuilderAssistant, context, template, dialect);
		}
	}

	private static class DeclaredQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final EvaluationContextProvider evaluationContextProvider;

		public DeclaredQueryLookupStrategy(MyBatisMapperBuilderAssistant mapperBuilderAssistant,
				MyBatisMappingContext context, SqlSessionTemplate template, Dialect dialect,
				EvaluationContextProvider evaluationContextProvider) {
			super(mapperBuilderAssistant, context, template, dialect);
			this.evaluationContextProvider = evaluationContextProvider;
		}

		@Override
		protected RepositoryQuery resolveQuery(MyBatisQueryMethod method,
				MyBatisMapperBuilderAssistant mapperBuilderAssistant, MyBatisMappingContext context,
				SqlSessionTemplate template, Dialect dialect, NamedQueries namedQueries) {
			// TODO
			// return new SimpleMyBatisQuery(method, context, template, dialect);

			throw new IllegalStateException(
					String.format("Did neither find a NamedQuery nor an annotated query for method %s!", method));

		}
	}

	private static class CreateIfNotFoundQueryLookupStrategy extends AbstractQueryLookupStrategy {

		private final DeclaredQueryLookupStrategy lookupStrategy;
		private final CreateQueryLookupStrategy createStrategy;

		public CreateIfNotFoundQueryLookupStrategy(MyBatisMapperBuilderAssistant mapperBuilderAssistant,
				MyBatisMappingContext context, SqlSessionTemplate template, Dialect dialect,
				CreateQueryLookupStrategy createStrategy, DeclaredQueryLookupStrategy lookupStrategy) {
			super(mapperBuilderAssistant, context, template, dialect);
			this.lookupStrategy = lookupStrategy;
			this.createStrategy = createStrategy;
		}

		@Override
		protected RepositoryQuery resolveQuery(MyBatisQueryMethod method,
				MyBatisMapperBuilderAssistant mapperBuilderAssistant, MyBatisMappingContext context,
				SqlSessionTemplate template, Dialect dialect, NamedQueries namedQueries) {
			try {
				return lookupStrategy.resolveQuery(method, mapperBuilderAssistant, context, template, dialect, namedQueries);
			} catch (IllegalStateException e) {
				return createStrategy.resolveQuery(method, mapperBuilderAssistant, context, template, dialect, namedQueries);
			}
		}
	}

	public static QueryLookupStrategy create(MyBatisMapperBuilderAssistant mapperBuilderAssistant,
			MyBatisMappingContext context, SqlSessionTemplate sqlSessionTemplate, Dialect dialect,
			@Nullable QueryLookupStrategy.Key key, EvaluationContextProvider evaluationContextProvider) {

		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		Assert.notNull(evaluationContextProvider, "EvaluationContextProvider must not be null!");

		switch (key != null ? key : QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND) {
			case CREATE:
				return new CreateQueryLookupStrategy(mapperBuilderAssistant, context, sqlSessionTemplate, dialect);
			case USE_DECLARED_QUERY:
				return new DeclaredQueryLookupStrategy(mapperBuilderAssistant, context, sqlSessionTemplate, dialect,
						evaluationContextProvider);
			case CREATE_IF_NOT_FOUND:
				return new CreateIfNotFoundQueryLookupStrategy(mapperBuilderAssistant, context, sqlSessionTemplate, dialect,
						new CreateQueryLookupStrategy(mapperBuilderAssistant, context, sqlSessionTemplate, dialect),
						new DeclaredQueryLookupStrategy(mapperBuilderAssistant, context, sqlSessionTemplate, dialect,
								evaluationContextProvider));
			default:
				throw new IllegalArgumentException(String.format("Unsupported query lookup strategy %s!", key));
		}
	}
}
