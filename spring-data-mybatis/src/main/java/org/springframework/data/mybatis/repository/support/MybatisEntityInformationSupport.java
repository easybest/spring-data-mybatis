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

import org.springframework.data.domain.Persistable;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.query.DefaultMybatisEntityMetadata;
import org.springframework.data.mybatis.repository.query.MybatisEntityMetadata;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.Assert;

/**
 * Base class for {@link MybatisEntityInformation} implementation to share common method
 * implementations.
 *
 * @param <T> entity type
 * @param <ID> entity id
 * @author JARVIS SONG
 * @since 1.0.0
 */
public abstract class MybatisEntityInformationSupport<T, ID> extends AbstractEntityInformation<T, ID>
		implements MybatisEntityInformation<T, ID> {

	private MybatisEntityMetadata<T> metadata;

	public MybatisEntityInformationSupport(Class<T> domainClass) {
		super(domainClass);
		this.metadata = new DefaultMybatisEntityMetadata<>(domainClass);
	}

	public static <T> MybatisEntityInformation<T, ?> getEntityInformation(Class<T> domainClass,
			MybatisMappingContext mappingContext) {

		Assert.notNull(domainClass, "Domain class must not be null!");
		Assert.notNull(mappingContext, "MappingContext must not be null!");

		if (Persistable.class.isAssignableFrom(domainClass)) {
			return new MybatisPersistableEntityInformation(domainClass, mappingContext);
		}
		return new MybatisMappingContextEntityInformation<>(domainClass, mappingContext);
	}

	@Override
	public String getEntityName() {
		return this.metadata.getEntityName();
	}

}
