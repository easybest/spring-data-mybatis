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

import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.mapping.model.Domain;
import org.springframework.util.Assert;

/**
 * Implementation of {@link org.springframework.data.repository.core.EntityInformation}
 * that uses {@link org.springframework.data.mapping.context.MappingContext} to find the
 * domain class's id property.
 *
 * @param <T> entity type
 * @param <ID> entity id
 * @author JARVIS SONG
 * @since 2.0.0
 */
public class MybatisMappingContextEntityInformation<T, ID> extends MybatisEntityInformationSupport<T, ID> {

	private final MybatisPersistentEntity<?> entity;

	private final Domain model;

	public MybatisMappingContextEntityInformation(Class<T> domainClass, MybatisMappingContext mappingContext) {
		super(domainClass);
		Assert.notNull(mappingContext, "MappingContext must not be null.");

		// this.entity = mappingContext.getRequiredPersistentEntity(domainClass);
		this.model = mappingContext.getRequiredDomain(domainClass);
		this.entity = this.model.getEntity();

	}

	public MybatisPersistentEntity<?> getEntity() {
		return this.entity;
	}

	@Override
	public boolean hasCompositeId() {
		return this.entity.isCompositePrimaryKey();
	}

	@Override
	public void initVersion(T entity) {
		if (this.entity.hasVersionProperty()) {
			MybatisPersistentProperty property = this.entity.getRequiredVersionProperty();
			Object val = null;
			if (property.getType() == Long.class) {
				val = 0L;
			}
			else if (property.getType() == Integer.class) {
				val = 0;
			}

			this.entity.getPropertyAccessor(entity).setProperty(property, val);
		}
	}

	@Override
	public String getEntityName() {
		return (null != this.entity.getName()) ? this.entity.getName() : super.getEntityName();
	}

	@Override
	public String getTableName() {
		return this.model.getTable().toString();
	}

	@Override
	public ID getId(T entity) {
		return (ID) this.entity.getIdentifierAccessor(entity).getIdentifier();
	}

	@Override
	public Class<ID> getIdType() {
		return (Class<ID>) this.entity.getIdClass();
	}

	@Override
	public boolean isNew(T entity) {
		if (!this.entity.hasVersionProperty() || this.entity.getVersionProperty().getActualType().isPrimitive()) {
			return super.isNew(entity);
		}
		Object version = this.entity.getPropertyAccessor(entity).getProperty(this.entity.getRequiredVersionProperty());
		return null == version;
	}

}
