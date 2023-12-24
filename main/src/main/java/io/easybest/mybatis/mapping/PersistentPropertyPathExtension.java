/*
 * Copyright 2019-2023 the original author or authors.
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

import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * .
 *
 * @author Jarvis Song
 */
public class PersistentPropertyPathExtension {

	private final EntityManager context;

	private final MybatisPersistentEntity<?> entity;

	private final @Nullable PersistentPropertyPath<? extends MybatisPersistentProperty> path;

	public PersistentPropertyPathExtension(EntityManager entityManager, MybatisPersistentEntity<?> entity) {

		Assert.notNull(entityManager, "EntityManager must not be null.");
		Assert.notNull(entity, "Entity must not be null.");

		this.context = entityManager;
		this.entity = entity;
		this.path = null;
	}

	public PersistentPropertyPathExtension(EntityManager entityManager,
			PersistentPropertyPath<? extends MybatisPersistentProperty> path) {

		Assert.notNull(entityManager, "EntityManager must not be null.");
		Assert.notNull(path, "Path must not be null.");
		Assert.notNull(path.getBaseProperty(), "Path must not be empty.");

		this.context = entityManager;
		this.entity = path.getBaseProperty().getOwner();
		this.path = path;
	}

	public boolean isEmbedded() {
		return null != this.path && this.path.getLeafProperty().isEmbeddable();
	}

	public PersistentPropertyPathExtension getParentPath() {

		if (this.path == null) {
			throw new IllegalStateException("The parent path of a root path is not defined.");
		}

		if (this.path.getLength() == 1) {
			return new PersistentPropertyPathExtension(this.context, this.entity);
		}

		return new PersistentPropertyPathExtension(this.context, this.path.getParentPath());
	}

	@Nullable
	public MybatisPersistentEntity<?> getLeafEntity() {
		return this.path == null ? this.entity
				: this.context.getPersistentEntity(this.path.getLeafProperty().getActualType());
	}

	public boolean isEntity() {
		return this.path == null || this.path.getLeafProperty().isEntity();
	}

	public boolean hasIdProperty() {

		MybatisPersistentEntity<?> leafEntity = this.getLeafEntity();
		return leafEntity != null && leafEntity.hasIdProperty();
	}

	public PersistentPropertyPath<? extends MybatisPersistentProperty> getRequiredPersistentPropertyPath() {

		Assert.state(this.path != null, "No path.");

		return this.path;
	}

}
