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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverrides;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.annotation.Fetch;
import org.springframework.data.mybatis.annotation.FetchMode;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 * @since 2.0.2
 */
public class Association implements Serializable {

	private static final long serialVersionUID = 2263465833694781795L;

	private static final Collection<Class<? extends Annotation>> ASSOCIATION_ANNOTATIONS;

	static {

		Set<Class<? extends Annotation>> annotations = new HashSet<>();
		annotations.add(OneToMany.class);
		annotations.add(OneToOne.class);
		annotations.add(ManyToMany.class);
		annotations.add(ManyToOne.class);
		annotations.add(ElementCollection.class);

		ASSOCIATION_ANNOTATIONS = Collections.unmodifiableSet(annotations);

	}
	protected final MybatisPersistentProperty property;

	protected final Domain self;

	protected final Domain target;

	protected final Connector connector;

	protected Map<String, String> embeddingOverrides = Collections.emptyMap();

	public Association(MybatisPersistentProperty sourceProperty, Domain self, Domain target) {
		this.property = sourceProperty;
		this.self = self;
		this.target = target;
		this.connector = this.processConnector();
		if (this.isEmbedding()) {
			this.processEmbeddingOverrides();
		}
	}

	public Identifier getEmbeddingOverrideColumn(Identifier column) {
		String override = this.embeddingOverrides.get(column.getText());
		if (null != override) {
			return Identifier.toIdentifier(override);
		}
		return column;
	}

	private void processEmbeddingOverrides() {
		AttributeOverrides attributeOverrides = this.property.findAnnotation(AttributeOverrides.class);
		if (null == attributeOverrides || null == attributeOverrides.value()
				|| attributeOverrides.value().length == 0) {
			return;
		}
		this.embeddingOverrides = Arrays.stream(attributeOverrides.value())
				.collect(Collectors.toMap(ao -> ao.name(), ao -> ao.column().name()));
	}

	private Connector processConnector() {
		if (this.property.isAnnotationPresent(OneToOne.class)) {
			OneToOne oneToOne = this.property.findAnnotation(OneToOne.class);
			// String mappedBy = (String) AnnotationUtils.getValue(oneToOne, "mappedBy");
			String mappedBy = oneToOne.mappedBy();
			if (StringUtils.hasText(mappedBy)) {
				MybatisPersistentProperty mappedProperty = this.target.getEntity()
						.getRequiredPersistentProperty(mappedBy);
				return this.getForeignKey(mappedProperty, true);
			}
			return this.getForeignKey(this.property, false);
		}
		if (this.property.isAnnotationPresent(ManyToOne.class)) {
			return this.getForeignKey(this.property, false);
		}
		if (this.property.isAnnotationPresent(OneToMany.class)) {
			OneToMany oneToMany = this.property.findAnnotation(OneToMany.class);
			String mappedBy = oneToMany.mappedBy();
			if (StringUtils.hasText(mappedBy)) {
				MybatisPersistentProperty mappedProperty = this.target.getEntity()
						.getRequiredPersistentProperty(mappedBy);
				return this.getForeignKey(mappedProperty, true);
			}
			// will use join table once no mappedBy
			JoinTable joinTable = new JoinTable();
			joinTable.setTable(this.getJoinTableName(this.property));
			return joinTable;
		}

		if (this.property.isAnnotationPresent(ManyToMany.class)) {
			ManyToMany manyToMany = this.property.findAnnotation(ManyToMany.class);
			String mappedBy = manyToMany.mappedBy();
			if (StringUtils.hasText(mappedBy)) {
				MybatisPersistentProperty mappedProperty = this.target.getEntity()
						.getRequiredPersistentProperty(mappedBy);
				return this.getJoinTable(mappedProperty, true);
			}
			else {
				return this.getJoinTable(this.property, false);
			}
		}

		return null;
	}

	private JoinTable getJoinTable(MybatisPersistentProperty property, boolean reverse) {
		JoinTable joinTable = new JoinTable();
		joinTable.setTable(this.getJoinTableName(property));
		if (property.isAnnotationPresent(javax.persistence.JoinTable.class)) {

			javax.persistence.JoinTable joinTableAnn = property.findAnnotation(javax.persistence.JoinTable.class);
			JoinColumn[] joinColumns = joinTableAnn.joinColumns();
			JoinColumn[] inverseJoinColumns = joinTableAnn.inverseJoinColumns();
			if (null != joinColumns && joinColumns.length > 0) {
				for (JoinColumn joinColumn : joinColumns) {
					if (reverse) {
						joinTable.addForeign(this.processJoinColumn(joinColumn, false));
					}
					else {
						joinTable.addLocal(this.processJoinColumn(joinColumn, false));
					}
				}
			}
			else {
				if (null == this.self.getPrimaryKey()) {
					throw new MappingException("No primary key of " + this.self.getEntity().getType());
				}

				this.self.getPrimaryKey().getColumns().values().stream().forEach(col -> {
					String localColumnName;

					Iterable<MybatisPersistentProperty> properties = this.target.getEntity()
							.getPersistentProperties(ManyToMany.class);
					Optional<MybatisPersistentProperty> mappedProperty = StreamUtils
							.createStreamFromIterator(properties.iterator()).filter(p -> {
								ManyToMany manyToManyAnn = p.getRequiredAnnotation(ManyToMany.class);
								return StringUtils.hasText(manyToManyAnn.mappedBy())
										&& manyToManyAnn.mappedBy().equals(property.getName());
							}).findFirst();
					if (mappedProperty.isPresent()) {
						localColumnName = mappedProperty.get().getName() + "_" + col.getName().getText();
					}
					else {
						localColumnName = this.self.getEntity().getType().getSimpleName().toLowerCase() + "_"
								+ col.getName().getText();
					}

					Column local = new Column(this.self, Identifier.toIdentifier(localColumnName));
					local.setJdbcType(col.getJdbcType());
					local.setJavaType(col.getJavaType());
					local.setTypeHandler(col.getTypeHandler());
					org.springframework.data.mybatis.mapping.model.JoinColumn joinColumn = new org.springframework.data.mybatis.mapping.model.JoinColumn(
							local, col);
					if (reverse) {
						joinTable.addForeign(joinColumn);
					}
					else {
						joinTable.addLocal(joinColumn);
					}
				});

			}
			if (null != inverseJoinColumns && inverseJoinColumns.length > 0) {
				for (JoinColumn joinColumn : inverseJoinColumns) {
					if (reverse) {
						joinTable.addLocal(this.processJoinColumn(joinColumn, false));
					}
					else {
						joinTable.addForeign(this.processJoinColumn(joinColumn, false));
					}
				}
			}
			else {
				if (null == this.target.getPrimaryKey()) {
					throw new MappingException("No primary key of " + this.target.getEntity().getType());
				}

				this.target.getPrimaryKey().getColumns().values().stream().forEach(col -> {
					String localColumnName = property.getName() + "_" + col.getName().getText();
					Column local = new Column(this.target, Identifier.toIdentifier(localColumnName));
					local.setJdbcType(col.getJdbcType());
					local.setJavaType(col.getJavaType());
					local.setTypeHandler(col.getTypeHandler());
					org.springframework.data.mybatis.mapping.model.JoinColumn joinColumn = new org.springframework.data.mybatis.mapping.model.JoinColumn(
							local, col);
					if (reverse) {
						joinTable.addForeign(joinColumn);
					}
					else {
						joinTable.addLocal(joinColumn);
					}
				});

			}
		}
		return joinTable;
	}

	private Table getJoinTableName(MybatisPersistentProperty property) {
		String schema = null;
		String catalog = null;
		String name = null;

		if (property.isAnnotationPresent(javax.persistence.JoinTable.class)) {
			javax.persistence.JoinTable joinTableAnn = property.findAnnotation(javax.persistence.JoinTable.class);
			schema = joinTableAnn.schema();
			catalog = joinTableAnn.catalog();
			name = joinTableAnn.name();
		}
		if (StringUtils.isEmpty(name)) {
			name = (property.getOwner().getType().getSimpleName() + "_" + property.getActualType().getSimpleName())
					.toLowerCase();
		}
		return new Table(schema, catalog, name);
	}

	private ForeignKey getForeignKey(MybatisPersistentProperty property, boolean reverse) {
		if (property.isAnnotationPresent(JoinColumns.class)) {
			JoinColumns joinColumns = property.findAnnotation(JoinColumns.class);
			if (null != joinColumns.value() && joinColumns.value().length > 0) {
				ForeignKey fk = new ForeignKey();
				for (JoinColumn joinColumn : joinColumns.value()) {
					fk.addJoinColumn(this.processJoinColumn(joinColumn, reverse));
				}
				return fk;
			}
			else {
				return this.getDefaultForeignKey(reverse);
			}
		}
		else if (property.isAnnotationPresent(JoinColumn.class)) {
			JoinColumn joinColumn = property.findAnnotation(JoinColumn.class);
			ForeignKey fk = new ForeignKey();
			fk.addJoinColumn(this.processJoinColumn(joinColumn, reverse));
			return fk;
		}
		else {
			return this.getDefaultForeignKey(reverse);
		}
	}

	private ForeignKey getDefaultForeignKey(boolean reverse) {
		PrimaryKey targetPrimaryKey = this.target.getPrimaryKey();
		if (null == targetPrimaryKey) {
			throw new MappingException(
					"No primary key of " + this.target.getTable() + " in association " + this.property);
		}
		ForeignKey fk = new ForeignKey();
		for (Column pk : targetPrimaryKey.getColumns().values()) {
			Column foreign = pk;
			Column local = new Column(this.self,
					Identifier.toIdentifier(this.property.getName() + "_" + pk.getName().getText()));
			if (reverse) {
				fk.addJoinColumn(new org.springframework.data.mybatis.mapping.model.JoinColumn(foreign, local));
			}
			else {
				fk.addJoinColumn(new org.springframework.data.mybatis.mapping.model.JoinColumn(local, foreign));
			}
		}
		return fk;
	}

	private org.springframework.data.mybatis.mapping.model.JoinColumn processJoinColumn(JoinColumn joinColumn,
			boolean reverse) {
		Column local;
		Column foreign;
		String referencedColumnName = joinColumn.referencedColumnName();
		if (StringUtils.hasText(referencedColumnName)) {
			Column referencedColumn = this.target.findColumn(Identifier.toIdentifier(referencedColumnName));
			if (null != referencedColumn) {
				foreign = referencedColumn;
			}
			else {
				foreign = new Column(this.target, Identifier.toIdentifier(referencedColumnName));
			}
		}
		else {
			if (null == this.target.getPrimaryKey() || this.target.getPrimaryKey().isComposited()) {
				throw new MappingException("Could not speculate referencedColumn in " + this.property);
			}
			foreign = this.target.getPrimaryKey().getColumns().values().iterator().next();
		}

		String columnName = joinColumn.name();
		if (StringUtils.isEmpty(columnName)) {
			if (null == this.target.getPrimaryKey() || this.target.getPrimaryKey().isComposited()) {
				throw new MappingException("Could not speculate referencedColumn in " + this.property);
			}
			columnName = this.property.getName() + "_"
					+ this.target.getPrimaryKey().getColumns().values().iterator().next().getName().getText();
		}
		local = new Column(this.self, Identifier.toIdentifier(columnName));
		if (reverse) {
			return new org.springframework.data.mybatis.mapping.model.JoinColumn(foreign, local);
		}
		return new org.springframework.data.mybatis.mapping.model.JoinColumn(local, foreign);
	}

	private String getMappedBy() {
		for (Class<? extends Annotation> annotationType : ASSOCIATION_ANNOTATIONS) {
			Annotation annotation = this.property.findAnnotation(annotationType);
			if (null == annotation) {
				continue;
			}
			String mappedBy = (String) AnnotationUtils.getValue(annotation, "mappedBy");
			if (StringUtils.hasText(mappedBy)) {
				return mappedBy;
			}
		}
		return null;
	}

	public boolean isJoin() {
		Fetch fetch = this.property.findAnnotation(Fetch.class);
		if (null == fetch || fetch.value() != FetchMode.JOIN) {
			return false;
		}
		return this.getFetchType() == FetchType.EAGER;
	}

	public FetchType getFetchType() {
		for (Class<? extends Annotation> annotationType : ASSOCIATION_ANNOTATIONS) {
			Annotation annotation = this.property.findAnnotation(annotationType);
			if (null == annotation) {
				continue;
			}
			FetchType fetchType = (FetchType) AnnotationUtils.getValue(annotation, "fetch");
			return fetchType;
		}
		return FetchType.LAZY;
	}

	public boolean isOneToOne() {
		return this.property.isAnnotationPresent(OneToOne.class);
	}

	public boolean isManyToOne() {
		return this.property.isAnnotationPresent(ManyToOne.class);
	}

	public boolean isToOne() {
		return this.isOneToOne() || this.isManyToOne();
	}

	public boolean isOneToMany() {
		return this.property.isAnnotationPresent(OneToMany.class);
	}

	public boolean isManyToMany() {
		return this.property.isAnnotationPresent(ManyToMany.class);
	}

	public boolean isEmbedding() {
		return this.property.isAnnotationPresent(Embedded.class)
				|| this.property.hasActualTypeAnnotation(Embeddable.class);
	}

	public MybatisPersistentProperty getProperty() {
		return this.property;
	}

	public Domain getSelf() {
		return this.self;
	}

	public Domain getTarget() {
		return this.target;
	}

	public Connector getConnector() {
		return this.connector;
	}

}
