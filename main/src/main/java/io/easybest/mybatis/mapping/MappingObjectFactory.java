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

package io.easybest.mybatis.mapping;

import java.io.Serializable;
import java.util.List;

import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;

import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Instantiate entity faster then {@link DefaultObjectFactory}.
 *
 * @author Jarvis Song
 */
public class MappingObjectFactory implements ObjectFactory, Serializable {

	private static final long serialVersionUID = -5670364349805959264L;

	private final EntityManager entityManager;

	private final EntityInstantiators instantiators;

	private final ObjectFactory delegate;

	public MappingObjectFactory(EntityManager entityManager) {
		this.entityManager = entityManager;
		this.instantiators = new EntityInstantiators();
		this.delegate = new DefaultObjectFactory();
	}

	@Override
	public <T> T create(Class<T> type) {

		return this.create(type, null, null);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {

		if (!this.entityManager.hasPersistentEntityFor(type)) {
			return this.delegate.create(type, constructorArgTypes, constructorArgs);
		}

		MybatisPersistentEntityImpl<?> entity = this.entityManager.getRequiredPersistentEntity(type);
		PreferredConstructor<?, MybatisPersistentPropertyImpl> persistenceConstructor = entity
				.getPersistenceConstructor();
		if (null == persistenceConstructor) {
			return this.delegate.create(type, constructorArgTypes, constructorArgs);
		}

		int argumentsSize = null == constructorArgTypes ? 0 : constructorArgTypes.size();
		if (persistenceConstructor.getParameterCount() != argumentsSize) {
			return this.delegate.create(type, constructorArgTypes, constructorArgs);
		}

		if (!CollectionUtils.isEmpty(constructorArgTypes)) {
			for (int i = 0; i < constructorArgTypes.size(); i++) {
				Parameter<Object, MybatisPersistentPropertyImpl> parameter = persistenceConstructor.getParameters()
						.get(i);
				Class<?> argType = constructorArgTypes.get(i);
				if (argType != parameter.getRawType()) {
					return this.create(type, constructorArgTypes, constructorArgs);
				}
			}
		}

		EntityInstantiator instantiator = this.instantiators.getInstantiatorFor(entity);
		return (T) instantiator.createInstance(entity,
				new MybatisParameterValueProvider(persistenceConstructor, constructorArgs));
	}

	@Override
	public <T> boolean isCollection(Class<T> type) {

		return this.delegate.isCollection(type);
	}

	static class MybatisParameterValueProvider implements ParameterValueProvider<MybatisPersistentPropertyImpl> {

		private final PreferredConstructor<?, MybatisPersistentPropertyImpl> persistenceConstructor;

		private final List<Object> constructorArgs;

		public MybatisParameterValueProvider(
				PreferredConstructor<?, MybatisPersistentPropertyImpl> persistenceConstructor,
				List<Object> constructorArgs) {
			this.persistenceConstructor = persistenceConstructor;
			this.constructorArgs = constructorArgs;
		}

		@Nullable
		@Override
		@SuppressWarnings({ "unchecked" })
		public <T> T getParameterValue(@NonNull Parameter<T, MybatisPersistentPropertyImpl> parameter) {
			if (null == this.constructorArgs) {
				return null;
			}
			int idx = this.persistenceConstructor.getParameters().indexOf(parameter);
			return (T) this.constructorArgs.get(idx);
		}

	}

}
