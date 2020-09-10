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

import java.util.Comparator;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.IdClass;

import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mybatis.mapping.model.Table;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link MybatisPersistentEntity}.
 *
 * @param <T> type of entity
 * @author JARVIS SONG
 */
class MybatisPersistentEntityImpl<T> extends BasicPersistentEntity<T, MybatisPersistentProperty>
		implements MybatisPersistentEntity<T> {

	private Lazy<Table> table;

	MybatisPersistentEntityImpl(TypeInformation<T> information) {
		this(information, null);
	}

	MybatisPersistentEntityImpl(TypeInformation<T> information, Comparator<MybatisPersistentProperty> comparator) {
		super(information, comparator);

		this.table = Lazy.of(() -> {

			String schema = null;
			String catalog = null;
			String name = null;
			if (this.isAnnotationPresent(javax.persistence.Table.class)) {
				javax.persistence.Table t = this.getRequiredAnnotation(javax.persistence.Table.class);
				schema = t.schema();
				catalog = t.catalog();
				name = t.name();
			}
			if (StringUtils.isEmpty(name)) {
				Entity entity = this.findAnnotation(Entity.class);
				name = ((null != entity) && StringUtils.hasText(entity.name())) ? entity.name()
						: this.getType().getSimpleName();
			}
			Table table = new Table(schema, catalog, name);
			return table;
		});
	}

	@Override
	public boolean isCompositePrimaryKey() {
		if (this.isAnnotationPresent(IdClass.class)) {
			return true;
		}

		return this.hasIdProperty() ? this.getRequiredIdProperty().isAnnotationPresent(EmbeddedId.class) : false;
	}

	@Override
	public Table getTable() {
		return this.table.get();
	}

	@Override
	public Class<?> getIdClass() {

		return this.isAnnotationPresent(IdClass.class) ? this.getRequiredAnnotation(IdClass.class).value()
				: (this.hasIdProperty() ? this.getRequiredIdProperty().getActualType() : null);
	}

	@Override
	protected MybatisPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(
			MybatisPersistentProperty property) {
		return property.isIdProperty() ? property : null;
	}

}
