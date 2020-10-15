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

import java.beans.Introspector;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Only basic columns.
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Domain implements Serializable {

	private static final long serialVersionUID = -5603024534312929025L;

	private final MybatisMappingContext mappingContext;

	private final MybatisPersistentEntity<?> entity;

	private Table table;

	private PrimaryKey primaryKey;

	private final Map<Identifier, Column> normalColumns = new LinkedHashMap<>();

	private final Map<MybatisPersistentProperty, Association> associations = new LinkedHashMap<>();

	public Domain(MybatisMappingContext mappingContext, Class<?> entityClass) {
		this.mappingContext = mappingContext;
		this.entity = mappingContext.getRequiredPersistentEntity(entityClass);
		this.internalInitialize();
	}

	private void internalInitialize() {
		this.initializeTable();
		this.initializePrimaryKey();
		this.initializeNormalColumns();
		this.initializeEmbeddings();

	}

	public void initialize() {

		this.initializeAssociations();

		//
	}

	public ColumnResult findColumnByPropertyName(String propertyName) {

		if (StringUtils.isEmpty(propertyName)) {
			return null;
		}

		String[] properties = propertyName.split("\\.");
		if (properties.length == 1) {
			if (null != this.primaryKey) {
				Optional<Column> column = this.primaryKey.getColumns().values().stream()
						.filter(c -> c.getPropertyName().equals(propertyName)).findFirst();
				if (column.isPresent()) {
					return new ColumnResult(this, column.get());
				}
			}

			Optional<Column> column = this.normalColumns.values().stream()
					.filter(c -> c.getPropertyName().equals(propertyName)).findFirst();
			if (column.isPresent()) {
				return new ColumnResult(this, column.get());
			}
			return null;
		}

		if (properties.length == 2) {
			if (null != this.primaryKey) {
				Optional<Column> column = this.primaryKey.getColumns().values().stream()
						.filter(c -> c.getPropertyName().equals(propertyName)).findFirst();
				if (column.isPresent()) {
					return new ColumnResult(this, column.get());
				}
			}
			return this.findColumnByPropertyName(properties);
		}

		return this.findColumnByPropertyName(properties);
	}

	private ColumnResult findColumnByPropertyName(String[] properties) {

		List<Association> associations = new LinkedList<>();
		Domain domain = this;
		for (int i = 0; i < properties.length - 1; i++) {
			domain = this.findAssociationByPropertyName(associations, domain, properties[i]);
		}
		if (associations.size() != properties.length - 1) {
			return null;
		}

		Association last = associations.get(associations.size() - 1);
		if (null != last.getTarget().getPrimaryKey()) {
			Optional<Column> column = last.getTarget().getPrimaryKey().getColumns().values().stream()
					.filter(c -> c.getPropertyName().equals(properties[properties.length - 1])).findFirst();
			if (column.isPresent()) {
				ColumnResult cr = new ColumnResult(this, column.get());
				cr.setAssociations(associations);
				return cr;
			}

			last.getTarget().getNormalColumns().values().stream()
					.filter(c -> c.getPropertyName().equals(properties[properties.length - 1])).findFirst();
			if (column.isPresent()) {
				ColumnResult cr = new ColumnResult(this, column.get());
				cr.setAssociations(associations);
				return cr;
			}

		}

		return null;
	}

	private Domain findAssociationByPropertyName(List<Association> associations, Domain domain, String propertyName) {
		Optional<Association> association = domain.getAssociations().entrySet().stream()
				.filter(entry -> entry.getValue().isToOne())
				.filter(entry -> propertyName.equals(entry.getKey().getName())).map(entry -> entry.getValue())
				.findFirst();
		if (!association.isPresent()) {
			return null;
		}

		Association ass = association.get();
		associations.add(ass);

		return ass.getTarget();
	}

	public Column findColumn(Identifier columnName) {
		if (null != this.primaryKey) {
			Column column = this.primaryKey.getColumns().get(columnName);
			if (null != column) {
				return column;
			}
		}

		Column column = this.normalColumns.get(columnName);
		return column;
	}

	public List<Column> findColumnByTypeHandler(Class<?> typeHandler) {

		List<Column> columns = new ArrayList<>();
		if (null != this.primaryKey) {
			columns.addAll(this.primaryKey.getColumns().values().stream().filter(c -> typeHandler == c.getTypeHandler())
					.collect(Collectors.toList()));
		}
		columns.addAll(this.normalColumns.values().stream().filter(c -> typeHandler == c.getTypeHandler())
				.collect(Collectors.toList()));

		this.associations.values().stream().filter(a -> a.isEmbedding()).forEach(a -> {
			columns.addAll(a.getTarget().getNormalColumns().values().stream()
					.filter(c -> typeHandler == c.getTypeHandler()).collect(Collectors.toList()));
		});
		return columns;
	}

	public String getTableAlias() {
		return Introspector.decapitalize(this.entity.getType().getSimpleName());
	}

	private void initializeEmbeddings() {
		this.associations.putAll(StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> !p.isIdProperty() && !p.isAssociation() && p.isEmbeddable())
				.collect(Collectors.toMap(p -> p, p -> {
					Domain domain = new Domain(this.mappingContext, p.getType());
					return new Embedding(p, this, domain);
				})));
	}

	private void initializeAssociations() {

		this.associations.putAll(StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> p.isAnnotationPresent(OneToOne.class))
				.collect(Collectors.toMap(p -> p, p -> new OneToOneAssociation(p, this,
						this.mappingContext.getRequiredDomain(p.getActualType())))));

		this.associations.putAll(StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> p.isAnnotationPresent(ManyToOne.class))
				.collect(Collectors.toMap(p -> p, p -> new ManyToOneAssociation(p, this,
						this.mappingContext.getRequiredDomain(p.getActualType())))));

		this.associations.putAll(StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> p.isAnnotationPresent(OneToMany.class))
				.collect(Collectors.toMap(p -> p, p -> new OneToManyAssociation(p, this,
						this.mappingContext.getRequiredDomain(p.getActualType())))));

		this.associations.putAll(StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> p.isAnnotationPresent(ManyToMany.class))
				.collect(Collectors.toMap(p -> p, p -> new ManyToManyAssociation(p, this,
						this.mappingContext.getRequiredDomain(p.getActualType())))));
	}

	private void initializeNormalColumns() {
		this.normalColumns.putAll(StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> !p.isIdProperty() && !p.isAssociation() && !p.isEmbeddable()).map(p -> {
					Column column = new Column(this, p);
					return column;
				}).collect(Collectors.toMap(Column::getName, c -> c)));

	}

	private void initializePrimaryKey() {
		if (!this.entity.hasIdProperty()) {
			return;
		}
		this.primaryKey = new PrimaryKey(this);
	}

	private void initializeTable() {
		String schema = null;
		String catalog = null;
		String name = null;
		if (this.entity.isAnnotationPresent(javax.persistence.Table.class)) {
			javax.persistence.Table tableAnn = this.entity.getRequiredAnnotation(javax.persistence.Table.class);
			schema = tableAnn.schema();
			catalog = tableAnn.catalog();
			name = tableAnn.name();
		}
		if (StringUtils.isEmpty(name)) {
			Entity entityAnn = this.entity.findAnnotation(Entity.class);
			name = ((null != entityAnn) && StringUtils.hasText(entityAnn.name())) ? entityAnn.name()
					: this.entity.getType().getSimpleName();
		}
		this.table = new Table(schema, catalog, name);
	}

	public MybatisPersistentEntity<?> getEntity() {
		return this.entity;
	}

	public MybatisMappingContext getMappingContext() {
		return this.mappingContext;
	}

	public Table getTable() {
		return this.table;
	}

	public PrimaryKey getPrimaryKey() {
		return this.primaryKey;
	}

	public Map<Identifier, Column> getNormalColumns() {
		return this.normalColumns;
	}

	public Map<MybatisPersistentProperty, Association> getAssociations() {
		return this.associations;
	}

}
