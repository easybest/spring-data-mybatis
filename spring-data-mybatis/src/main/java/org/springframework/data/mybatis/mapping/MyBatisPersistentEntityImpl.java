package org.springframework.data.mybatis.mapping;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.ParsingUtils;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.mybatis.mapping.MyBatisPersistentPropertyImpl.*;

/**
 * @author Jarvis Song
 */
public class MyBatisPersistentEntityImpl<T> extends BasicPersistentEntity<T, MyBatisPersistentProperty>
		implements MyBatisPersistentEntity<T> {

	private org.springframework.data.mybatis.mapping.Table table = new Table();
	private boolean inited = false;

	////////////////////////////////

	private final MyBatisMappingContext mappingContext;

	public MyBatisPersistentEntityImpl(MyBatisMappingContext context, TypeInformation<T> information) {
		this(context, information, null);
	}

	public MyBatisPersistentEntityImpl(MyBatisMappingContext context, TypeInformation<T> information,
			Comparator<MyBatisPersistentProperty> comparator) {
		super(information, comparator);

		/////////////////////////////////////////
		mappingContext = context;
	}

	public void initTable() {

		if (!isAnnotationPresent(Entity.class) && !isAnnotationPresent(Embeddable.class)) {
			return;
		}
		if (this.inited) {
			return;
		}
		this.inited = true;
		this.table.setEntity(this);
		processClassLevel();
		processColumns();

	}

	private void processClassLevel() {
		javax.persistence.Table tableAnnotation = findAnnotation(javax.persistence.Table.class);
		String catalog = null, schema = null, name = null;
		if (null != tableAnnotation) {
			if (StringUtils.hasText(tableAnnotation.catalog())) {
				catalog = tableAnnotation.catalog();
			}
			if (StringUtils.hasText(tableAnnotation.schema())) {
				schema = tableAnnotation.schema();
			}
			if (StringUtils.hasText(tableAnnotation.name())) {
				name = tableAnnotation.name();
			}

			if (null != tableAnnotation.indexes() && tableAnnotation.indexes().length > 0) {
				Stream.of(tableAnnotation.indexes()).forEach(table::addIndex);
			}
			if (null != tableAnnotation.uniqueConstraints() && tableAnnotation.uniqueConstraints().length > 0) {
				Stream.of(tableAnnotation.uniqueConstraints()).forEach(table::addUniqueConstraint);
			}
		}
		if (null == name) {
			Entity entityAnnotation = findAnnotation(Entity.class);
			if (null != entityAnnotation && StringUtils.hasText(entityAnnotation.name())) {
				name = entityAnnotation.name();
			}
		}

		if (null == name) {
			name = ParsingUtils.reconcatenateCamelCase(getType().getSimpleName(), "_");
		}

		table.setCatalog(catalog);
		table.setSchema(schema);
		table.setName(name);
		table.setAlias(StringUtils.uncapitalize(getType().getSimpleName()));
	}

	private String getColumnName(MyBatisPersistentProperty property) {

		for (Class<? extends Annotation> annotationType : UPDATEABLE_ANNOTATIONS) {

			Annotation annotation = property.findAnnotation(annotationType);

			if (null == annotation) {
				continue;
			}

			String name = (String) AnnotationUtils.getValue(annotation, "name");
			if (StringUtils.hasText(name)) {
				return name;
			}
		}

		return ParsingUtils.reconcatenateCamelCase(property.getName(), "_");
	}

	private void processColumns() {
		this.processNormalColumns(this);
	}

	private void processNormalColumns(MyBatisPersistentEntityImpl<?> entity) {
		Table table = entity.getTable();

		if (entity.isAnnotationPresent(IdClass.class)) {
			CompositeIdColumn compositeIdColumn = new CompositeIdColumn(table);
			table.setIdColumn(compositeIdColumn);
		}

		entity.doWithProperties((SimplePropertyHandler) p -> {
			MyBatisPersistentPropertyImpl property = (MyBatisPersistentPropertyImpl) p;

			if (entity.isAnnotationPresent(IdClass.class) && property.isAnnotationPresent(Id.class)) {
				// @IdClass composite id mode
				CompositeIdColumn compositeIdColumn = (CompositeIdColumn) table.getIdColumn();
				IdColumn idColumn = new IdColumn(table);
				idColumn.setName(getColumnName(property));
				idColumn.setPrefix(StringUtils.uncapitalize(entity.getType().getSimpleName()));
				idColumn.setAlias(property.getName());
				idColumn.setProperty(property);
				idColumn.setColumnAnnotation(property.findAnnotation(javax.persistence.Column.class));
				compositeIdColumn.addIdColumn(idColumn);
				property.setMappedColumn(idColumn);
				return;
			}

			if (property.isAnnotationPresent(Id.class) && property.isEntity()
					&& property.getType().isAnnotationPresent(Embeddable.class)) {
				// @Id + @Embeddable composite id mode
				compositeIdColumn(table, property);
				return;
			}

			if (property.isAnnotationPresent(Id.class) && property.isAnnotationPresent(GeneratedValue.class)) {
				GeneratedValue generatedValue = property.getRequiredAnnotation(GeneratedValue.class);
				GeneratedValueIdColumn idColumn = new GeneratedValueIdColumn(table);
				idColumn.setStrategy(generatedValue.strategy());
				idColumn.setGenerator(generatedValue.generator());
				idColumn.setName(getColumnName(property));
				idColumn.setPrefix(StringUtils.uncapitalize(entity.getType().getSimpleName()));
				idColumn.setAlias(property.getName());
				idColumn.setProperty(property);
				idColumn.setColumnAnnotation(property.findAnnotation(javax.persistence.Column.class));
				table.setIdColumn(idColumn);
				property.setMappedColumn(idColumn);
				return;
			}

			if (property.isAnnotationPresent(Id.class)) {
				// @Id
				IdColumn idColumn = new IdColumn(table);
				idColumn.setName(getColumnName(property));
				idColumn.setPrefix(StringUtils.uncapitalize(entity.getType().getSimpleName()));
				idColumn.setAlias(property.getName());
				idColumn.setProperty(property);
				idColumn.setColumnAnnotation(property.findAnnotation(javax.persistence.Column.class));
				table.setIdColumn(idColumn);
				property.setMappedColumn(idColumn);
				return;
			}

			Column column = new Column(table);
			column.setName(getColumnName(property));
			column.setPrefix(table.getAlias());
			column.setAlias(property.getName());
			column.setProperty(property);
			column.setColumnAnnotation(property.findAnnotation(javax.persistence.Column.class));
			table.addColumn(column);
			property.setMappedColumn(column);

		});

		entity.doWithAssociations((SimpleAssociationHandler) association -> {
			MyBatisPersistentPropertyImpl property = (MyBatisPersistentPropertyImpl) association.getInverse();
			if (property.isAnnotationPresent(EmbeddedId.class)) {
				// @EmbeddableId composite id mode
				compositeIdColumn(table, property);
				return;
			}
			if (property.isAnnotationPresent(Embedded.class)) {
				if (!property.isEntity()) {
					throw new MappingException(property + " set @Embedded, but is not a entity.");
				}
				MyBatisPersistentEntityImpl<?> targetEntity = mappingContext
						.getRequiredPersistentEntity(property.getActualType());
				if (!targetEntity.isInited()) {
					targetEntity.initTable();
				}
				Table targetTable = targetEntity.getTable();
				Map<String, String> attributeOverrides = new HashMap<>();
				if (property.isAnnotationPresent(AttributeOverrides.class)) {
					attributeOverrides = Stream.of(property.getRequiredAnnotation(AttributeOverrides.class).value())
							.filter(ao -> StringUtils.hasText(ao.column().name()))
							.collect(Collectors.toMap(ao -> ao.name(), ao -> ao.column().name()));
				}

				Map<String, String> finalAttributeOverrides = attributeOverrides;
				targetTable.getColumnsIncludeId().stream().forEach(c -> {
					Column column = c.clone();
					if (finalAttributeOverrides.containsKey(column.getAlias())) {
						column.setName(finalAttributeOverrides.get(column.getAlias()));
					}
					column.setAlias(property.getName() + '.' + column.getAlias());
					column.setPrefix(StringUtils.uncapitalize(entity.getType().getSimpleName()));
					table.addColumn(column);
				});
				return;
			}
			if (property.isAnnotationPresent(OneToOne.class) || property.isAnnotationPresent(ManyToOne.class)) {
				if (!property.isEntity()) {
					throw new MappingException(property + " set @Embedded, but is not a entity.");
				}
				MyBatisPersistentEntityImpl<?> targetEntity = mappingContext
						.getRequiredPersistentEntity(property.getActualType());
				if (!targetEntity.isInited()) {
					targetEntity.initTable();
				}

				Table targetTable = targetEntity.getTable();
				ToOneAssociation toOneAssociation;
				if (property.isAnnotationPresent(JoinTable.class)) {
					toOneAssociation = new ToOneJoinTableAssociation(table, targetTable, property);
				} else {
					toOneAssociation = new ToOneJoinColumnAssociation(table, targetTable, property);
				}
				table.addAssociation(toOneAssociation);
				property.setMappedAssociation(toOneAssociation);
				return;
			}
			if (property.isAnnotationPresent(ElementCollection.class)) {
				Table targetTable = null;
				if (property.isEntity()) {
					MyBatisPersistentEntityImpl<?> targetEntity = mappingContext
							.getRequiredPersistentEntity(property.getActualType());
					if (!targetEntity.isInited()) {
						targetEntity.initTable();
					}
					targetTable = targetEntity.getTable();
				}
				ElementCollectionAssociation elementCollectionAssociation = new ElementCollectionAssociation(table, targetTable,
						property);
				table.addAssociation(elementCollectionAssociation);
				property.setMappedAssociation(elementCollectionAssociation);
				return;
			}
			if (property.isAnnotationPresent(OneToMany.class) || property.isAnnotationPresent(ManyToMany.class)) {
				MyBatisPersistentEntityImpl<?> targetEntity = mappingContext
						.getRequiredPersistentEntity(property.getActualType());
				if (!targetEntity.isInited()) {
					targetEntity.initTable();
				}
				Table targetTable = targetEntity.getTable();
				ToManyJoinTableAssociation toManyJoinTableAssociation = new ToManyJoinTableAssociation(table, targetTable,
						property);
				table.addAssociation(toManyJoinTableAssociation);
				property.setMappedAssociation(toManyJoinTableAssociation);
				return;
			}

		});

	}

	boolean isInited() {
		return inited;
	}

	private void compositeIdColumn(Table table, MyBatisPersistentPropertyImpl property) {
		CompositeIdColumn compositeIdColumn = new CompositeIdColumn(table);
		MyBatisPersistentEntityImpl<?> targetEntity = mappingContext.getPersistentEntity(property.getType());
		targetEntity.doWithProperties((SimplePropertyHandler) idP -> {
			MyBatisPersistentPropertyImpl idProperty = (MyBatisPersistentPropertyImpl) idP;
			IdColumn idColumn = new IdColumn(table);
			idColumn.setName(getColumnName(idProperty));
			idColumn.setPrefix(StringUtils.uncapitalize(getType().getSimpleName()) + "."
					+ StringUtils.uncapitalize(targetEntity.getType().getSimpleName()));
			idColumn.setAlias(StringUtils.uncapitalize(targetEntity.getType().getSimpleName()) + "." + idProperty.getName());
			idColumn.setProperty(idProperty);
			idColumn.setColumnAnnotation(idProperty.findAnnotation(javax.persistence.Column.class));
			idProperty.setMappedColumn(idColumn);
			compositeIdColumn.addIdColumn(idColumn);
		});
		table.setIdColumn(compositeIdColumn);
		property.setMappedColumn(compositeIdColumn);
	}

	@Override
	public Table getTable() {
		return this.table;
	}

	@Override
	protected MyBatisPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(
			MyBatisPersistentProperty property) {

		return property.isIdProperty() ? property : null;
	}

}
