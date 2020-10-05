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
package org.springframework.data.mybatis.mapping.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.util.StreamUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class CompositePrimaryKey implements PrimaryKey {

	private final Map<String, Column> columns = new LinkedHashMap<>();

	private final Model model;

	public CompositePrimaryKey(Model model, MybatisPersistentEntity<?> entity) {
		this.model = model;

		StreamUtils.createStreamFromIterator(entity.iterator()).filter(PersistentProperty::isIdProperty).forEach(p -> {

			if (p.isEmbeddable()) {
				MybatisPersistentEntity<?> embeddedEntity = model.getMappingContext()
						.getRequiredPersistentEntity(p.getType());
				embeddedEntity.forEach(ep -> {

					Column column = new Column(model, ep);
					column.setPropertyName(p.getName() + '.' + ep.getName());
					this.addColumn(column);
				});
			}
			else {
				Column column = new Column(model, p);
				this.addColumn(column);
			}

		});

	}

	public void addColumn(Column column) {
		this.columns.put(column.getName().getCanonicalName(), column);
	}

	@Override
	public boolean isComposited() {
		return true;
	}

	@Override
	public Collection<Column> getColumns() {
		return this.columns.values();
	}

	@Override
	public Class<?> getType() {
		return this.model.getMappingEntity().getIdClass();
	}

	@Override
	public Column findColumn(String name) {
		return this.columns.get(name);
	}

	@Override
	public Model getModel() {
		return this.model;
	}

}
