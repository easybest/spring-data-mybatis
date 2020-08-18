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
import java.util.List;
import java.util.Map;

import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link MappingContext} implementation based on JPA annotations.
 *
 * @author JARVIS SONG
 * @since 1.0.0
 */
public class MybatisMappingContext
		extends AbstractMappingContext<MybatisPersistentEntityImpl<?>, MybatisPersistentProperty> {

	private FieldNamingStrategy fieldNamingStrategy;

	private Map<String, String> namedQueries = new HashMap<>();

	private MultiValueMap<Class<?>, Class<?>> entityRepositoryMapping = new LinkedMultiValueMap<>();

	@Override
	protected <T> MybatisPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {

		MybatisPersistentEntityImpl<T> entity = new MybatisPersistentEntityImpl(typeInformation);

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

		return entity;
	}

	@Override
	protected MybatisPersistentProperty createPersistentProperty(Property property,
			MybatisPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new MybatisPersistentPropertyImpl(property, owner, simpleTypeHolder, this.fieldNamingStrategy);
	}

	public void setFieldNamingStrategy(FieldNamingStrategy fieldNamingStrategy) {
		this.fieldNamingStrategy = fieldNamingStrategy;
	}

	public String getNamedQuery(String name) {
		return this.namedQueries.get(name);
	}

	public void setEntityRepositoryMapping(MultiValueMap<Class<?>, Class<?>> entityRepositoryMapping) {
		this.entityRepositoryMapping = entityRepositoryMapping;
	}

	public Class<?> getRepositoryInterface(Class<?> entityClass) {
		List<Class<?>> repositories = this.entityRepositoryMapping.get(entityClass);
		if (CollectionUtils.isEmpty(repositories)) {
			return null;
		}

		if (repositories.size() == 1) {
			return repositories.get(0);
		}

		return repositories.stream().filter(r -> MybatisRepository.class.isAssignableFrom(r)).findFirst()
				.orElse(repositories.get(0));

	}

}
