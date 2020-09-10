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
package org.springframework.data.mybatis.repository.query;

import javax.persistence.CascadeType;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.util.Assert;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class CascadeHandler implements InitializingBean {

	private final MappingCascadeBeanWrapperFactory factory;

	public CascadeHandler(
			MappingContext<? extends PersistentEntity<?, ?>, ? extends PersistentProperty<?>> mappingContext) {
		this(PersistentEntities.of(mappingContext));
	}

	public CascadeHandler(PersistentEntities entities) {

		Assert.notNull(entities, "PersistentEntities must not be null!");

		this.factory = new MappingCascadeBeanWrapperFactory(entities);
	}

	public <T> T touch(T target, CascadeType cascadeType) {
		return target;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

}
