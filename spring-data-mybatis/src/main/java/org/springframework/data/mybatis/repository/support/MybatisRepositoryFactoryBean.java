/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.query.EscapeCharacter;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Special adapter for Springs {@link org.springframework.beans.factory.FactoryBean}
 * interface to allow easy setup of * repository factories via Spring configuration.
 *
 * @param <T> the type of the repository
 * @param <ID> id class
 * @param <S> entity
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class MybatisRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

	private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;

	private @Nullable SqlSessionTemplate sqlSessionTemplate;

	private @Nullable MybatisMappingContext mappingContext;

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
		Assert.state(null != this.mappingContext, "MybatisMappingContext must not be null!");
		Assert.state(null != this.sqlSessionTemplate, "SqlSessionTemplate must not be null!");

		super.afterPropertiesSet();
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {
		return this.createRepositoryFactory(this.mappingContext, this.sqlSessionTemplate);
	}

	private RepositoryFactorySupport createRepositoryFactory(MybatisMappingContext mappingContext,
			SqlSessionTemplate sqlSessionTemplate) {
		Assert.state(null != this.mappingContext, "MybatisMappingContext must not be null!");
		Assert.state(null != this.sqlSessionTemplate, "SqlSessionTemplate must not be null!");

		MybatisRepositoryFactory repositoryFactory = new MybatisRepositoryFactory(mappingContext, sqlSessionTemplate);
		repositoryFactory.setEscapeCharacter(this.escapeCharacter);
		return repositoryFactory;
	}

	@Override
	public void setMappingContext(MappingContext<?, ?> mappingContext) {
		super.setMappingContext(mappingContext);
		this.mappingContext = (MybatisMappingContext) mappingContext;
	}

	public void setEscapeCharacter(char escapeCharacter) {
		this.escapeCharacter = EscapeCharacter.of(escapeCharacter);
	}

	public void setSqlSessionTemplate(@Nullable SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
	}

}
