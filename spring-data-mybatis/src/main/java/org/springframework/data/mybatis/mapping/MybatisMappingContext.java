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
package org.springframework.data.mybatis.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;

import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.internal.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.springframework.data.mybatis.dialect.internal.StandardDialectResolver;
import org.springframework.data.mybatis.mapping.model.Domain;
import org.springframework.data.mybatis.precompiler.SimpleMybatisPrecompiler;
import org.springframework.data.util.TypeInformation;

/**
 * {@link MappingContext} implementation based on JPA annotations.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class MybatisMappingContext extends
		AbstractMappingContext<MybatisPersistentEntityImpl<?>, MybatisPersistentProperty> implements InitializingBean {

	private static final SnakeCaseFieldNamingStrategy SNAKE_CASE_FIELD_NAMING_STRATEGY = new SnakeCaseFieldNamingStrategy();

	private final SqlSessionTemplate sqlSessionTemplate;

	private final Dialect dialect;

	private FieldNamingStrategy fieldNamingStrategy;

	private Map<String, String> namedQueries = new HashMap<>();

	private Map<Class<?>, Domain> domains = new HashMap<>();

	public MybatisMappingContext(SqlSessionTemplate sqlSessionTemplate) {
		this.sqlSessionTemplate = sqlSessionTemplate;
		this.dialect = StandardDialectResolver.INSTANCE.resolveDialect(new DatabaseMetaDataDialectResolutionInfoAdapter(
				sqlSessionTemplate.getConfiguration().getEnvironment().getDataSource()));
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		if (null == this.fieldNamingStrategy) {
			if (null != this.sqlSessionTemplate
					&& this.sqlSessionTemplate.getConfiguration().isMapUnderscoreToCamelCase()) {
				this.fieldNamingStrategy = SNAKE_CASE_FIELD_NAMING_STRATEGY;
			}
			else {
				this.fieldNamingStrategy = PropertyNameFieldNamingStrategy.INSTANCE;
			}
		}

		this.domains = this.getManagedTypes().stream().map(TypeInformation::getType)
				.filter(clz -> clz.isAnnotationPresent(Entity.class))
				.collect(Collectors.toMap(clz -> clz, clz -> new Domain(this, clz)));

		this.domains.values().stream().forEach(Domain::initialize);

		// generate mybatis mappers
		this.domains.values().stream().forEach(domain -> new SimpleMybatisPrecompiler(this, domain).compile());
	}

	public Domain getDomain(Class<?> entityClass) {
		return this.domains.get(entityClass);
	}

	public Domain getRequiredDomain(Class<?> entityClass) {
		Domain domain = this.getDomain(entityClass);
		if (null == domain) {
			throw new MappingException("Could not find Domain for type: " + entityClass);
		}
		return domain;
	}

	@Override
	protected <T> MybatisPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		MybatisPersistentEntityImpl<T> entity = new MybatisPersistentEntityImpl<>(typeInformation);
		this.processNamedQueries(entity);
		return entity;
	}

	@Override
	protected MybatisPersistentProperty createPersistentProperty(Property property,
			MybatisPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {

		return new MybatisPersistentPropertyImpl(property, owner, simpleTypeHolder);
	}

	public String getNamedQuery(String name) {
		return this.namedQueries.get(name);
	}

	private void processNamedQueries(MybatisPersistentEntityImpl<?> entity) {
		javax.persistence.NamedQuery namedQuery = entity.findAnnotation(javax.persistence.NamedQuery.class);
		if ((null != namedQuery)) {
			this.namedQueries.put(namedQuery.name(), namedQuery.query());
		}
		NamedQueries namedQueries = entity.findAnnotation(NamedQueries.class);
		if (null != namedQueries) {
			for (javax.persistence.NamedQuery nq : namedQueries.value()) {
				this.namedQueries.put(nq.name(), nq.query());

			}
		}
		NamedNativeQuery namedNativeQuery = entity.findAnnotation(NamedNativeQuery.class);
		if (null != namedNativeQuery) {
			this.namedQueries.put(namedNativeQuery.name(), namedNativeQuery.query());
		}
		NamedNativeQueries namedNativeQueries = entity.findAnnotation(NamedNativeQueries.class);
		if (null != namedNativeQueries) {
			for (NamedNativeQuery nq : namedNativeQueries.value()) {
				this.namedQueries.put(nq.name(), nq.query());
			}
		}
	}

	public void setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	public SqlSessionTemplate getSqlSessionTemplate() {
		return this.sqlSessionTemplate;
	}

	public Dialect getDialect() {
		return this.dialect;
	}

	public FieldNamingStrategy getFieldNamingStrategy() {
		return this.fieldNamingStrategy;
	}

}
