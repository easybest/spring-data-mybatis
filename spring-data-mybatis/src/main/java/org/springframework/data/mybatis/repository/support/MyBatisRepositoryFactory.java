package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

import static org.springframework.data.querydsl.QuerydslUtils.*;

/**
 * @author Jarvis Song
 */
public class MyBatisRepositoryFactory extends RepositoryFactorySupport {

	private SqlSessionTemplate sqlSessionTemplate;

	public MyBatisRepositoryFactory(SqlSessionTemplate sqlSessionTemplate) {
		Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null!");
		this.sqlSessionTemplate = sqlSessionTemplate;

	}

	@Override
	public <T, ID> MyBatisEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		return (MyBatisEntityInformation<T, ID>) MyBatisEntityInformationSupport.getEntityInformation(domainClass);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation information) {

		SimpleMyBatisRepository<?, ?> repository = getTargetRepository(information, sqlSessionTemplate);

		return repository;
	}

	protected SimpleMyBatisRepository<?, ?> getTargetRepository(RepositoryInformation information,
			SqlSessionTemplate sqlSessionTemplate) {

		MyBatisEntityInformation<?, Object> entityInformation = getEntityInformation(information.getDomainType());
		return getTargetRepositoryViaReflection(information, entityInformation, sqlSessionTemplate);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		if (isQueryDslExecutor(metadata.getRepositoryInterface())) {
			return QuerydslMyBatisRepository.class;
		} else {
			return SimpleMyBatisRepository.class;
		}
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
