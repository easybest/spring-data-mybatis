package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.repository.query.MyBatisQueryLookupStrategy;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.util.Assert;

import java.util.Optional;

import static org.springframework.data.querydsl.QuerydslUtils.*;

/**
 * @author Jarvis Song
 */
public class MyBatisRepositoryFactory extends RepositoryFactorySupport {

	private final SqlSessionTemplate sqlSessionTemplate;
	private final MyBatisMappingContext mappingContext;

	public MyBatisRepositoryFactory(SqlSessionTemplate sqlSessionTemplate, MyBatisMappingContext mappingContext,
			Dialect dialect) {

		super();

		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		Assert.notNull(dialect, "Dialect must not be null!");

		this.sqlSessionTemplate = sqlSessionTemplate;
		this.mappingContext = mappingContext;

		addRepositoryProxyPostProcessor(new MyBatisMapperBuildProcessor(sqlSessionTemplate, mappingContext, dialect));
		addQueryCreationListener(new MyBatisQueryCreationListener(sqlSessionTemplate, mappingContext, dialect));

	}

	@Override
	public <T, ID> MyBatisEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		return (MyBatisEntityInformation<T, ID>) MyBatisEntityInformationSupport.getEntityInformation(domainClass);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation information) {

		SimpleMyBatisRepository<?, ?> repository = getTargetRepository(sqlSessionTemplate, information);
		return repository;
	}

	protected SimpleMyBatisRepository<?, ?> getTargetRepository(SqlSessionTemplate sqlSessionTemplate,
			RepositoryInformation information) {

		MyBatisEntityInformation<?, Object> entityInformation = getEntityInformation(information.getDomainType());

		return getTargetRepositoryViaReflection(information, sqlSessionTemplate, information, entityInformation);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		if (isQueryDslExecutor(metadata.getRepositoryInterface())) {
			return QuerydslMyBatisRepository.class;
		} else {
			return SimpleMyBatisRepository.class;
		}
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
			EvaluationContextProvider evaluationContextProvider) {

		return Optional
				.of(MyBatisQueryLookupStrategy.create(sqlSessionTemplate, mappingContext, key, evaluationContextProvider));
	}

	/**
	 * Returns whether the given repository interface requires a QueryDsl specific implementation to be chosen.
	 *
	 * @param repositoryInterface
	 * @return
	 */
	private boolean isQueryDslExecutor(Class<?> repositoryInterface) {

		return QUERY_DSL_PRESENT && QuerydslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
	}
}
