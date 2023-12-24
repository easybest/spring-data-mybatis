/*
 * Copyright 2019-2022 the original author or authors.
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

package io.easybest.mybatis.repository.support;

import java.io.Serializable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;

import io.easybest.mybatis.mapping.EntityManager;

/**
 * .
 *
 * @author Jarvis Song
 * @param <S> repository
 * @param <T> entity type
 * @param <ID> primary key
 *
 */
public class MybatisRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
		extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> implements ApplicationEventPublisherAware {

	private ApplicationEventPublisher publisher;

	private BeanFactory beanFactory;

	private EntityCallbacks entityCallbacks;

	private EntityManager entityManager;

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

		if (null != this.beanFactory) {
			this.entityCallbacks = EntityCallbacks.create(this.beanFactory);
		}

		super.afterPropertiesSet();
	}

	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {

		MybatisRepositoryFactory factory = new MybatisRepositoryFactory(this.entityManager, this.publisher);
		factory.setEntityCallbacks(this.entityCallbacks);
		factory.setBeanFactory(this.beanFactory);

		return factory;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {

		super.setApplicationEventPublisher(publisher);

		this.publisher = publisher;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {

		super.setBeanFactory(beanFactory);

		this.beanFactory = beanFactory;
	}

	public void setEntityManager(EntityManager entityManager) {

		this.entityManager = entityManager;

		this.setMappingContext(entityManager);
	}

}
