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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Stream;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.MybatisExampleRepository;
import org.springframework.data.mybatis.repository.query.EscapeCharacter;
import org.springframework.data.mybatis.repository.query.MybatisQueryLookupStrategy;
import org.springframework.data.mybatis.repository.query.MybatisQueryPrepareProcessor;
import org.springframework.data.mybatis.repository.query.MybatisRepositoryPrepareProcessor;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * MyBatis specific generic repository factory.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class MybatisRepositoryFactory extends RepositoryFactorySupport {

	private final MybatisMappingContext mappingContext;

	private final SqlSessionTemplate sqlSessionTemplate;

	private AuditingHandler auditingHandler;

	private EntityPathResolver entityPathResolver;

	private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;

	public MybatisRepositoryFactory(MybatisMappingContext mappingContext, SqlSessionTemplate sqlSessionTemplate) {
		this.mappingContext = mappingContext;
		this.sqlSessionTemplate = sqlSessionTemplate;
		this.entityPathResolver = SimpleEntityPathResolver.INSTANCE;

		this.addRepositoryProxyPostProcessor((factory, repositoryInformation) -> {
			if (hasMethodReturningStream(repositoryInformation.getRepositoryInterface())) {
				factory.addAdvice(SurroundingTransactionDetectorMethodInterceptor.INSTANCE);
			}
		});

		this.addRepositoryProxyPostProcessor(
				new MybatisRepositoryPrepareProcessor(mappingContext, sqlSessionTemplate.getConfiguration()));
		this.addQueryCreationListener(
				new MybatisQueryPrepareProcessor(mappingContext, sqlSessionTemplate.getConfiguration()));

	}

	private static boolean hasMethodReturningStream(Class<?> repositoryClass) {

		Method[] methods = ReflectionUtils.getAllDeclaredMethods(repositoryClass);

		for (Method method : methods) {
			if (Stream.class.isAssignableFrom(method.getReturnType())) {
				return true;
			}
		}

		return false;
	}

	@Override
	public <T, ID> MybatisEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
		return (MybatisEntityInformation<T, ID>) MybatisEntityInformationSupport.getEntityInformation(domainClass,
				this.mappingContext);
	}

	@Override
	protected final MybatisRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation metadata) {
		MybatisRepositoryImplementation<?, ?> repository = this.getTargetRepository(metadata, this.sqlSessionTemplate,
				this.auditingHandler);
		repository.setEscapeCharacter(this.escapeCharacter);
		return repository;
	}

	protected MybatisRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation information,
			SqlSessionTemplate sqlSessionTemplate, AuditingHandler auditingHandler) {
		MybatisEntityInformation<?, Serializable> entityInformation = this
				.getEntityInformation(information.getDomainType());
		Object repository = this.getTargetRepositoryViaReflection(information, entityInformation, information,
				sqlSessionTemplate, auditingHandler);
		Assert.isInstanceOf(MybatisRepositoryImplementation.class, repository);
		return (MybatisRepositoryImplementation<?, ?>) repository;
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		if (MybatisExampleRepository.class.isAssignableFrom(metadata.getRepositoryInterface())) {
			return SimpleMybatisExampleRepository.class;
		}
		return SimpleMybatisRepository.class;
	}

	@Override
	protected ProjectionFactory getProjectionFactory(ClassLoader classLoader, BeanFactory beanFactory) {
		return super.getProjectionFactory(classLoader, beanFactory);
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		return Optional.of(MybatisQueryLookupStrategy.create(this.sqlSessionTemplate, this.mappingContext, key,
				evaluationContextProvider, this.escapeCharacter));
	}

	@Override
	protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
		return super.getRepositoryFragments(metadata);
	}

	public void setEntityPathResolver(EntityPathResolver entityPathResolver) {
		Assert.notNull(entityPathResolver, "EntityPathResolver must not be null.");
		this.entityPathResolver = entityPathResolver;
	}

	public void setEscapeCharacter(EscapeCharacter escapeCharacter) {
		this.escapeCharacter = escapeCharacter;
	}

	public void setAuditingHandler(AuditingHandler auditingHandler) {
		this.auditingHandler = auditingHandler;
	}

}
