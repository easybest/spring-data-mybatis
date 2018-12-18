package org.springframework.data.mybatis.repository.support;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import org.mybatis.spring.SqlSessionTemplate;

/**
 * * Special adapter for Springs {@link org.springframework.beans.factory.FactoryBean}
 * interface to allow easy setup of * repository factories via Spring configuration.
 *
 * @param <T>
 * @param <S>
 * @param <ID>
 */
public class MybatisRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

	private @Nullable SqlSessionTemplate sqlSessionTemplate;

	private MappingContext<?, ?> mappingContext;

	private Boolean supportMultipleDatasources;

	/**
	 * Creates a new {@link TransactionalRepositoryFactoryBeanSupport} for the given
	 * repository interface.
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public MybatisRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	public void afterPropertiesSet() {

		Assert.state(null != sqlSessionTemplate, "SqlSessionTemplate must not be null!");

		super.afterPropertiesSet();
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {

		Assert.state(null != sqlSessionTemplate, "SqlSessionTemplate must not be null!");

		return new MybatisRepositoryFactory(sqlSessionTemplate, mappingContext);
	}

	public void setSqlSessionTemplate(@Nullable SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	public void setSupportMultipleDatasources(Boolean supportMultipleDatasources) {
		this.supportMultipleDatasources = supportMultipleDatasources;
	}

	@Override
	public void setMappingContext(MappingContext<?, ?> mappingContext) {
		super.setMappingContext(mappingContext);
		this.mappingContext = mappingContext;
	}

}
