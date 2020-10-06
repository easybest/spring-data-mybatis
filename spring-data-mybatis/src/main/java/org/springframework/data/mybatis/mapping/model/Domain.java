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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Domain implements Model {

	private static final long serialVersionUID = 4850421315538416089L;

	protected final MybatisMappingContext mappingContext;

	protected final MybatisPersistentEntity<?> entity;

	protected final String alias;

	protected Table table;

	protected Model owner;

	protected PrimaryKey primaryKey;

	protected Map<String, Column> normalColumns; // columnName -> column

	protected Map<String, VersionColumn> versionColumns;

	protected List<EmbeddedDomain> embeddings;

	protected List<OneToOneAssociation> oneToOneAssociations;

	protected List<ManyToOneAssociation> manyToOneAssociations;

	protected List<OneToManyAssociation> oneToManyAssociations;

	protected List<ManyToManyAssociation> manyToManyAssociations;

	public Domain(MybatisMappingContext mappingContext, Model owner, Class<?> entityClass, String alias) {
		this.mappingContext = mappingContext;
		this.owner = owner;
		this.entity = mappingContext.getRequiredPersistentEntity(entityClass);
		if (null != owner) {
			this.alias = owner.getAlias() + '.' + alias;
		}
		else {
			this.alias = alias;
		}

		this.internalInitialize();
	}

	public void initialize() {
		if (null != this.owner) {
			return;
		}

		this.initializeOneToOneAssociations();
		this.initializeManyToOneAssociations();
		this.initializeOneToManyAssociations();
		this.initializeManyToManyAssociations();
	}

	private void internalInitialize() {
		this.initializeTable();
		this.initializePrimaryKey();
		this.initializeNormalColumns();
		this.initializeVersionColumns();
		this.initializeEmbeddedDomains();
	}

	private void initializeManyToManyAssociations() {
		this.manyToManyAssociations = StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> p.isAnnotationPresent(ManyToMany.class))
				.map(p -> new ManyToManyAssociation(this.mappingContext, this, p, p.getName()))
				.collect(Collectors.toList());
	}

	private void initializeOneToManyAssociations() {
		this.oneToManyAssociations = StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> p.isAnnotationPresent(OneToMany.class))
				.map(p -> new OneToManyAssociation(this.mappingContext, this, p, p.getName()))
				.collect(Collectors.toList());
	}

	private void initializeOneToOneAssociations() {
		this.oneToOneAssociations = StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> p.isAnnotationPresent(OneToOne.class))
				.map(p -> new OneToOneAssociation(this.mappingContext, this, p, p.getName()))
				.collect(Collectors.toList());
	}

	private void initializeManyToOneAssociations() {
		this.manyToOneAssociations = StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> p.isAnnotationPresent(ManyToOne.class))
				.map(p -> new ManyToOneAssociation(this.mappingContext, this, p, p.getName()))
				.collect(Collectors.toList());
	}

	private void initializeEmbeddedDomains() {
		this.embeddings = StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> !p.isIdProperty() && !p.isAssociation() && p.isEmbeddable())
				.map(p -> new EmbeddedDomain(this.mappingContext, this, p, p.getName())).collect(Collectors.toList());
	}

	private void initializeVersionColumns() {
		this.versionColumns = StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> p.isVersionProperty()).map(p -> new VersionColumn(this, p))
				.collect(Collectors.toMap(c -> c.getName().getCanonicalName(), c -> c));

	}

	private void initializeNormalColumns() {
		this.normalColumns = StreamUtils.createStreamFromIterator(this.entity.iterator())
				.filter(p -> !p.isIdProperty() && !p.isAssociation() && !p.isEmbeddable() && !p.isVersionProperty())
				.map(p -> new Column(this, p)).collect(Collectors.toMap(c -> c.getName().getCanonicalName(), c -> c));
	}

	private void initializePrimaryKey() {
		if (!this.entity.hasIdProperty()) {
			return;
		}
		if (!this.entity.isCompositePrimaryKey()) {
			this.primaryKey = new SinglePrimaryKey(this, this.entity.getRequiredIdProperty());
			return;
		}

		// composited primary key
		this.primaryKey = new CompositePrimaryKey(this, this.entity);
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

	@Override
	public Column findColumn(String name) {
		if (null != this.primaryKey) {
			Column column = this.primaryKey.findColumn(name);
			if (null != column) {
				return column;
			}
		}
		if (!CollectionUtils.isEmpty(this.normalColumns)) {
			Column column = this.normalColumns.get(name);
			if (null != column) {
				return column;
			}
		}
		if (!CollectionUtils.isEmpty(this.versionColumns)) {
			Column column = this.versionColumns.get(name);
			if (null != column) {
				return column;
			}
		}
		if (!CollectionUtils.isEmpty(this.embeddings)) {
			for (EmbeddedDomain embeddedDomain : this.embeddings) {
				Column c = embeddedDomain.findColumn(name);
				if (null != c) {
					return c;
				}
			}

		}

		return null;
	}

	@Override
	public Column findColumnByPropertyName(String propertyName) {
		Column column = null;
		if (null != this.primaryKey) {
			column = this.primaryKey.getColumns().stream().filter(c -> c.getPropertyName().equals(propertyName))
					.findFirst().orElse(null);
		}
		if (null == column && !CollectionUtils.isEmpty(this.normalColumns)) {
			column = this.normalColumns.values().stream().filter(c -> c.getPropertyName().equals(propertyName))
					.findFirst().orElse(null);
		}
		if (null == column && !CollectionUtils.isEmpty(this.versionColumns)) {
			column = this.versionColumns.values().stream().filter(c -> c.getPropertyName().equals(propertyName))
					.findFirst().orElse(null);
		}
		if (null == column && !CollectionUtils.isEmpty(this.embeddings)) {
			column = this.embeddings.stream()
					.map(embeddedDomain -> embeddedDomain.findColumnByPropertyName(propertyName)).filter(c -> null != c)
					.findFirst().orElse(null);
		}
		return column;
	}

	@Override
	public Column findColumn(PropertyPath propertyPath) {
		if (null == propertyPath) {
			return null;
		}
		String path = propertyPath.toDotPath();
		return this.findColumnByPropertyName(path);
	}

	@Override
	public List<Column> findColumnByTypeHandler(Class<?> typeHandlerClass) {
		List<Column> columns = new ArrayList<>();
		if (null != this.primaryKey) {
			columns.addAll(this.primaryKey.getColumns().stream().filter(c -> c.getTypeHandler() == typeHandlerClass)
					.collect(Collectors.toList()));
		}

		if (null != this.normalColumns) {
			columns.addAll(this.normalColumns.values().stream().filter(c -> c.getTypeHandler() == typeHandlerClass)
					.collect(Collectors.toList()));
		}

		if (null != this.versionColumns) {
			columns.addAll(this.versionColumns.values().stream().filter(c -> c.getTypeHandler() == typeHandlerClass)
					.collect(Collectors.toList()));
		}

		if (!CollectionUtils.isEmpty(this.embeddings)) {
			for (EmbeddedDomain embeddedDomain : this.embeddings) {
				columns.addAll(embeddedDomain.findColumnByTypeHandler(typeHandlerClass));
			}

		}

		return columns;
	}

	@Override
	public MybatisMappingContext getMappingContext() {
		return this.mappingContext;
	}

	@Override
	public String getAlias() {
		return this.alias;
	}

	@Override
	public Model getOwner() {
		return this.owner;
	}

	@Override
	public Table getTable() {
		return this.table;
	}

	@Override
	public MybatisPersistentEntity<?> getMappingEntity() {
		return this.entity;
	}

	@Override
	public PrimaryKey getPrimaryKey() {
		return this.primaryKey;
	}

	@Override
	public Collection<Component> getNormalColumns() {
		return (Collection) this.normalColumns.values();
	}

	@Override
	public Collection<Component> getVersionColumns() {
		return (Collection) this.versionColumns.values();
	}

	@Override
	public Collection<Embedding> getEmbeddings() {
		return (Collection) this.embeddings;
	}

	@Override
	public Collection<Association> getOneToOneAssociations() {
		return (Collection) this.oneToOneAssociations;
	}

	@Override
	public Collection<Association> getManyToOneAssociations() {
		return (Collection) this.manyToOneAssociations;
	}

	@Override
	public Collection<Association> getOneToManyAssociations() {
		return (Collection) this.oneToManyAssociations;
	}

	@Override
	public Collection<Association> getManyToManyAssociations() {
		return (Collection) this.manyToManyAssociations;
	}

	public String getTableAlias() {
		return this.alias;
	}

}
