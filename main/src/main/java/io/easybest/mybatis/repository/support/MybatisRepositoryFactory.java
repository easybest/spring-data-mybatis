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

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.repository.QuerydslMybatisPredicateExecutor;
import io.easybest.mybatis.repository.query.MybatisQueryIntendListener;
import io.easybest.mybatis.repository.query.MybatisQueryLookupStrategy;
import io.easybest.mybatis.repository.query.Procedure;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.PersistentEntityInformation;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import static org.springframework.data.querydsl.QuerydslUtils.QUERY_DSL_PRESENT;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisRepositoryFactory extends RepositoryFactorySupport {

	private final EntityManager entityManager;

	private EntityPathResolver entityPathResolver;

	private final ApplicationEventPublisher publisher;

	private EntityCallbacks entityCallbacks;

	public MybatisRepositoryFactory(EntityManager entityManager, ApplicationEventPublisher publisher) {

		Assert.notNull(entityManager, "EntityManager must not be null!");
		Assert.notNull(publisher, "ApplicationEventPublisher must not be null!");

		this.entityManager = entityManager;
		this.publisher = publisher;
		this.entityPathResolver = SimpleEntityPathResolver.INSTANCE;

		this.addRepositoryProxyPostProcessor((factory, repositoryInformation) -> {
			if (isTransactionNeeded(repositoryInformation.getRepositoryInterface())) {
				factory.addAdvice(SurroundingTransactionDetectorMethodInterceptor.INSTANCE);
			}
		});

		this.addQueryCreationListener(MybatisQueryIntendListener.INSTANCE);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		MybatisPersistentEntityImpl<?> entity = this.entityManager.getRequiredPersistentEntity(domainClass);

		return (EntityInformation<T, ID>) new PersistentEntityInformation<>(entity);
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation metadata) {

		MybatisPersistentEntityImpl<?> persistentEntity = this.entityManager
				.getRequiredPersistentEntity(metadata.getDomainType());
		return this.getTargetRepositoryViaReflection(metadata, this.entityManager, this.entityCallbacks,
				persistentEntity);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleMybatisRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		return Optional.of(MybatisQueryLookupStrategy.create(this.entityManager, key, evaluationContextProvider));

	}

	@Override
	protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {

		return this.getRepositoryFragments(metadata, this.entityManager, this.entityPathResolver);
	}

	private RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata, EntityManager entityManager,
			EntityPathResolver resolver) {

		boolean isQueryDslRepository = QUERY_DSL_PRESENT
				&& QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());

		if (!isQueryDslRepository) {
			return RepositoryFragments.empty();
		}

		if (metadata.isReactiveRepository()) {
			throw new InvalidDataAccessApiUsageException(
					"Cannot combine Querydsl and reactive repository support in a single interface");
		}

		// TODO QueryDSL

		return RepositoryFragments.just(new QuerydslMybatisPredicateExecutor<>(
				this.getEntityInformation(metadata.getDomainType()), entityManager, resolver));

	}

	public void setEntityCallbacks(EntityCallbacks entityCallbacks) {
		this.entityCallbacks = entityCallbacks;
	}

	public void setEntityPathResolver(EntityPathResolver entityPathResolver) {
		this.entityPathResolver = entityPathResolver;
	}

	private static boolean isTransactionNeeded(Class<?> repositoryClass) {
		Method[] methods = ReflectionUtils.getAllDeclaredMethods(repositoryClass);
		for (Method method : methods) {
			if (Stream.class.isAssignableFrom(method.getReturnType()) || method.isAnnotationPresent(Procedure.class)) {
				return true;
			}
		}
		return false;
	}

}
