package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.mapping.MyBatisMappingContext;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Special adapter for Springs {@link org.springframework.beans.factory.FactoryBean} interface to allow easy setup of
 * repository factories via Spring configuration.
 * 
 * @author Jarvis Song
 */
public class MyBatisRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

	private @Nullable SqlSessionTemplate sqlSessionTemplate;
	private @Nullable MyBatisMappingContext mappingContext;
	private @Nullable Dialect dialect;

	/**
	 * Creates a new {@link TransactionalRepositoryFactoryBeanSupport} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public MyBatisRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {
		return new MyBatisRepositoryFactory(sqlSessionTemplate, mappingContext, dialect);
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(sqlSessionTemplate != null, "SqlSessionTemplate must not be null!");
		Assert.state(dialect != null, "Dialect must not be null!");
		Assert.state(mappingContext != null, "MyBatisMappingContext must not be null!");

		super.afterPropertiesSet();
	}

	public void setSqlSessionTemplate(@Nullable SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

	public void setDialect(@Nullable Dialect dialect) {
		this.dialect = dialect;
	}

	public void setMappingContext(@Nullable MyBatisMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}
}
