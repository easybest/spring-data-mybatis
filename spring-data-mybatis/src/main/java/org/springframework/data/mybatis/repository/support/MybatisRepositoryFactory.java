package org.springframework.data.mybatis.repository.support;

import java.util.Optional;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mybatis.repository.query.MybatisQueryLookupStrategy;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.util.Assert;

import org.mybatis.spring.SqlSessionTemplate;

public class MybatisRepositoryFactory extends RepositoryFactorySupport {

	private final SqlSessionTemplate sqlSessionTemplate;

	public MybatisRepositoryFactory(SqlSessionTemplate sqlSessionTemplate,
			MappingContext<?, ?> mappingContext) {

		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");

		this.sqlSessionTemplate = sqlSessionTemplate;

		addRepositoryProxyPostProcessor(new MybatisMapperBuildProcessor(
				sqlSessionTemplate.getConfiguration(), mappingContext));
		addQueryCreationListener(new MybatisQueryCreationListener(
				sqlSessionTemplate.getConfiguration(), mappingContext));

	}

	@Override
	public <T, ID> MybatisEntityInformation<T, ID> getEntityInformation(
			Class<T> domainClass) {
		return (MybatisEntityInformation<T, ID>) MybatisEntityInformationSupport
				.getEntityInformation(domainClass);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {
		return getTargetRepositoryViaReflection(metadata, sqlSessionTemplate, metadata,
				getEntityInformation(metadata.getDomainType()));
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleMybatisRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		return Optional.of(MybatisQueryLookupStrategy.create(sqlSessionTemplate, key,
				evaluationContextProvider));
	}

}
