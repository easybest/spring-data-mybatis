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

package io.easybest.mybatis.mapping.precompile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.GenerationType;

import io.easybest.mybatis.auxiliary.Syntax;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisAssociation;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.precompile.ResultMap.Association;
import io.easybest.mybatis.mapping.precompile.ResultMap.ResultMapping;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;
import io.easybest.mybatis.repository.query.criteria.CriteriaQuery;
import io.easybest.mybatis.repository.query.criteria.DefaultCriteriaQuery;
import io.easybest.mybatis.repository.query.criteria.DefaultDeleteQuery;
import io.easybest.mybatis.repository.query.criteria.DefaultInsertQuery;
import io.easybest.mybatis.repository.query.criteria.DefaultUpdateQuery;
import io.easybest.mybatis.repository.query.criteria.DeleteQuery;
import io.easybest.mybatis.repository.query.criteria.InsertQuery;
import io.easybest.mybatis.repository.query.criteria.ParamValue;
import io.easybest.mybatis.repository.query.criteria.UpdateQuery;
import io.easybest.mybatis.repository.query.criteria.impl.ConditionsImpl;
import io.easybest.mybatis.repository.support.ResidentStatementName;
import org.apache.ibatis.mapping.ResultFlag;

import org.springframework.data.mapping.PersistentPropertyPath;

import static io.easybest.mybatis.mapping.precompile.SQL.COUNTS;
import static io.easybest.mybatis.repository.support.MybatisContext.PARAM_INSTANCE_PREFIX;
import static io.easybest.mybatis.repository.support.ResidentStatementName.BASE_RESULT_MAP;
import static io.easybest.mybatis.repository.support.ResidentStatementName.BASIC_RESULT_MAP;
import static io.easybest.mybatis.repository.support.ResidentStatementName.COUNT;
import static io.easybest.mybatis.repository.support.ResidentStatementName.COUNT_ALL;
import static io.easybest.mybatis.repository.support.ResidentStatementName.COUNT_QUERY_BY_EXAMPLE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_ALL;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_BY_ENTITIES;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_BY_ENTITY;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_BY_ID;
import static io.easybest.mybatis.repository.support.ResidentStatementName.DELETE_BY_IDS;
import static io.easybest.mybatis.repository.support.ResidentStatementName.EXISTS_BY_EXAMPLE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.EXISTS_BY_ID;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_ALL;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_ALL_WITH_SORT;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_CRITERIA;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_ID;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_IDS;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_PAGE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.INSERT;
import static io.easybest.mybatis.repository.support.ResidentStatementName.INSERT_SELECTIVE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.LAZY_RESULT_MAP;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_BY_EXAMPLE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_BY_EXAMPLE_WITH_PAGE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_BY_EXAMPLE_WITH_SORT;
import static io.easybest.mybatis.repository.support.ResidentStatementName.RESULT_MAP;
import static io.easybest.mybatis.repository.support.ResidentStatementName.UPDATE_BY_ID;
import static io.easybest.mybatis.repository.support.ResidentStatementName.UPDATE_SELECTIVE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.UPDATE_SELECTIVE_BY_ID;

/**
 * .
 *
 * @author Jarvis Song
 */
public class MybatisSimpleMapperSnippet extends MybatisMapperSnippet {

	private final EntityManager entityManager;

	private final StagingMappers stagingMappers;

	private final MybatisPersistentEntityImpl<?> entity;

	public MybatisSimpleMapperSnippet(EntityManager entityManager, StagingMappers stagingMappers,
			MybatisPersistentEntityImpl<?> entity) {

		this.entityManager = entityManager;
		this.stagingMappers = stagingMappers;
		this.entity = entity;
	}

	public Fragment pureTableName() {

		return Fragment.builder().id(ResidentStatementName.TABLE_NAME_PURE).contents(Collections.singletonList(Table.of(
				this.entity.getTableName().getReference(this.entityManager.getDialect().getIdentifierProcessing()))))
				.build();
	}

	public Fragment tableName() {

		return Fragment.builder().id(ResidentStatementName.TABLE_NAME).contents(Collections.singletonList(Table.base(
				this.entity.getTableName().getReference(this.entityManager.getDialect().getIdentifierProcessing()))))
				.build();
	}

	public Fragment pureColumnList() {

		ResultMap baseResultMap = this.baseResultMap();
		List<Column> columns = baseResultMap.getResultMappings().stream().map(rm -> Column.of(rm.getColumn()))
				.collect(Collectors.toList());

		columns.addAll(this.basicResultMap().getResultMappings().stream().map(rm -> Column.of(rm.getColumn()))
				.collect(Collectors.toList()));

		return Fragment.builder().id(ResidentStatementName.COLUMN_LIST_PURE)
				.contents(Collections
						.singletonList(SQL.of(columns.stream().map(Column::toString).collect(Collectors.joining(",")))))
				.build();
	}

	public Fragment columnList() {

		ResultMap baseResultMap = this.baseResultMap();
		List<Column> columns = baseResultMap.getResultMappings().stream().map(rm -> Column.base(rm.getColumn()))
				.collect(Collectors.toList());

		columns.addAll(this.basicResultMap().getResultMappings().stream().map(rm -> Column.base(rm.getColumn()))
				.collect(Collectors.toList()));

		return Fragment
				.builder().id(ResidentStatementName.COLUMN_LIST).contents(Collections.singletonList(SQL.of(columns
						.stream().map(c -> c.toString() + " AS " + c.getValue()).collect(Collectors.joining(",")))))
				.build();
	}

	public Fragment columnListUsingType() {

		String content = Stream.concat(this.baseResultMap().getResultMappings().stream(),
				this.basicResultMap().getResultMappings().stream()).map(rm -> {
					Column column = Column.base(rm.getColumn());
					SqlIdentifier property = SqlIdentifier.quoted(rm.getProperty());
					return column + " AS " + property.toSql(this.entityManager.getDialect().getIdentifierProcessing());
				}).collect(Collectors.joining(","));

		return Fragment.builder().id(ResidentStatementName.COLUMN_LIST_USING_TYPE)
				.contents(Collections.singletonList(SQL.of(content))).build();

	}

	public ResultMap baseResultMap() {

		List<ResultMapping> mappings = this.entityManager
				.findPersistentPropertyPaths(this.entity.getType(), p -> !p.isAssociation()).stream()
				.filter(ppp -> !ppp.getRequiredLeafProperty().isEntity() && null != ppp.getBaseProperty()).map(ppp -> {
					MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();
					MybatisPersistentPropertyImpl base = ppp.getBaseProperty();
					return ResultMapping.builder().property(ppp.toDotPath())
							.column(leaf.getRequiredColumnName()
									.getReference(this.entityManager.getDialect().getIdentifierProcessing()))
							.resultFlag(base.isIdProperty() ? ResultFlag.ID : null).property(ppp.toDotPath())
							.jdbcType(leaf.getJdbcType()).javaType(leaf.getJavaType())
							.typeHandler(leaf.getTypeHandler()).build();
				}).collect(Collectors.toList());

		return ResultMap.builder().id(BASE_RESULT_MAP).type(this.entity.getType().getName()).resultMappings(mappings)
				.build();
	}

	public ResultMap basicResultMap() {

		List<ResultMapping> mappings = new ArrayList<>();

		this.entityManager
				.findPersistentPropertyPaths(this.entity.getType(), MybatisPersistentPropertyImpl::isAssociation)
				.forEach(ppp -> {

					MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();
					MybatisAssociation association = leaf.getRequiredAssociation();

					if (!association.isOwningSide() || !association.isToOne()) {
						return;
					}

					association.getJoinColumns().stream().filter(
							jc -> null != jc.getReferencedPropertyPath() && !jc.getReferencedPropertyPath().isEmpty())
							.forEach(jc -> {
								MybatisPersistentPropertyImpl subLeaf = jc.getReferencedPropertyPath()
										.getRequiredLeafProperty();

								ResultMapping mapping = ResultMapping.builder()
										.property(leaf.getName() + '.' + jc.getReferencedPropertyPath().toDotPath())
										.column(jc.getColumnName().getReference(
												this.entityManager.getDialect().getIdentifierProcessing()))
										.javaType(subLeaf.getJavaType()).jdbcType(subLeaf.getJdbcType())
										.typeHandler(subLeaf.getTypeHandler()).build();
								mappings.add(mapping);

							});

				});

		return ResultMap.builder().id(BASIC_RESULT_MAP).type(this.entity.getType().getName()).extend(BASE_RESULT_MAP)
				.resultMappings(mappings).build();
	}

	public ResultMap lazyResultMap() {

		List<Association> associations = new ArrayList<>();

		this.entityManager
				.findPersistentPropertyPaths(this.entity.getType(), MybatisPersistentPropertyImpl::isAssociation)
				.stream().filter(ppp -> null != ppp.getBaseProperty()).forEach(ppp -> {

					MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();
					MybatisAssociation association = leaf.getRequiredAssociation();

					if (association.isToOne()) {
						associations.add(this.associationResultMapping(ppp));
					}

				});

		return ResultMap.builder().id(LAZY_RESULT_MAP).type(this.entity.getType().getName()).extend(BASE_RESULT_MAP)
				.associations(associations).build();
	}

	public ResultMap resultMap() {

		List<Association> associations = new ArrayList<>();
		List<ResultMap.Collection> collections = new ArrayList<>();

		this.entityManager
				.findPersistentPropertyPaths(this.entity.getType(), MybatisPersistentPropertyImpl::isAssociation)
				.stream().filter(ppp -> null != ppp.getBaseProperty()).forEach(ppp -> {

					MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();
					MybatisAssociation association = leaf.getRequiredAssociation();

					if (association.isToOne()) {

						if (association.isUseJoin()) {

							Association ass = Association.builder().property(ppp.toDotPath())
									.javaType(association.getTargetType().getName())
									.resultMap(association.getTargetType().getName() + '.' + LAZY_RESULT_MAP)
									.columnPrefix(ppp.toDotPath() + '.').build();
							associations.add(ass);

						}
						else {

							associations.add(this.associationResultMapping(ppp));

						}

					}
					else if (association.isToMany()) {

						// ResultMap.Collection collection =
						// ResultMap.Collection.builder().property(ppp.toDotPath())
						// .ofType(association.getTargetType().getName())
						// .fetchType(null != association.getFetchType()
						// ? association.getFetchType().name().toLowerCase() : null)
						// .column(association.getSelectMappingColumn()).select(association.getSelectStatementId())
						// .build();
						// collections.add(collection);
						//
						// this.stagingMappers.addAssociation(association);

					}

				});

		return ResultMap.builder().id(RESULT_MAP).type(this.entity.getType().getName()).extend(BASE_RESULT_MAP)
				.associations(associations).collections(collections).build();

	}

	private Association associationResultMapping(PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp) {

		MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();
		MybatisAssociation association = leaf.getRequiredAssociation();

		return Association.builder().property(ppp.toDotPath()).javaType(association.getTargetType().getName())
				.select(association.getSelectStatementId()).column(association.getSelectMappingColumn())
				.fetchType(null != association.getFetchType() ? association.getFetchType().name().toLowerCase() : null)
				.build();

	}

	public Insert insert(boolean selective) {

		DefaultInsertQuery<?, ParamValue> query = InsertQuery.create(this.entity.getType());
		query.selectKey();
		if (selective) {
			query.selective();
		}

		for (PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp : this.entityManager
				.findPersistentPropertyPaths(this.entity.getType(), p -> true)) {
			MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();
			if (leaf.isAssociation()) {

				MybatisAssociation association = leaf.getRequiredAssociation();
				if (association.isOwningSide() && association.isToOne()) {
					for (MybatisAssociation.JoinColumn jc : association.getJoinColumns()) {
						if (null == jc.getReferencedPropertyPath()) {
							continue;
						}

						Column column = Column.builder()
								.value(jc.getColumnName()
										.getReference(this.entityManager.getDialect().getIdentifierProcessing()))
								.build();
						query.set(column, ParamValue.of(PARAM_INSTANCE_PREFIX + ppp.toDotPath() + '.'
								+ jc.getReferencedPropertyPath().toDotPath(), null));

					}

				}

			}
			else if (!leaf.isEntity()) {

				if (!leaf.isWritable()) {
					continue;
				}

				if (leaf.isDatabaseDefaultValue()) {
					continue;
				}

				if (leaf.isIdProperty()) {

					if (this.entity.getGenerationType() == GenerationType.IDENTITY) {
						continue;
					}
				}

				Column column = Column.builder().value(leaf.getRequiredColumnName()
						.getReference(this.entityManager.getDialect().getIdentifierProcessing())).build();
				Parameter val = Parameter.builder().property(PARAM_INSTANCE_PREFIX + ppp.toDotPath())
						.javaType(leaf.getJavaType()).jdbcType(leaf.getJdbcType()).typeHandler(leaf.getTypeHandler())
						.build();

				if (leaf.isIdProperty()) {

					query.customSet(column.toString(), val.toString());
				}
				else {
					query.set(column, ParamValue.of(PARAM_INSTANCE_PREFIX + ppp.toDotPath(), null, leaf));
				}
			}
		}

		return query.presupposed(this.entityManager, this.entity, selective ? INSERT_SELECTIVE : INSERT, null, null);

	}

	public Update update(boolean selective, boolean byId) {

		if (!this.entity.hasIdProperty()) {
			return null;
		}

		DefaultUpdateQuery<?, ParamValue> query = UpdateQuery.create(this.entity.getType());

		if (selective) {
			query.selective();
		}

		for (PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp : this.entityManager
				.findPersistentPropertyPaths(this.entity.getType(), p -> true)) {
			MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();
			if (leaf.isAssociation()) {

				MybatisAssociation association = leaf.getRequiredAssociation();
				if (association.isOwningSide() && association.isToOne()) {
					for (MybatisAssociation.JoinColumn jc : association.getJoinColumns()) {
						if (null == jc.getReferencedPropertyPath()) {
							continue;
						}

						Column column = Column.of(jc.getColumnName()
								.getReference(this.entityManager.getDialect().getIdentifierProcessing()));

						query.set(column, ParamValue.of(PARAM_INSTANCE_PREFIX + ppp.toDotPath() + '.'
								+ jc.getReferencedPropertyPath().toDotPath(), null));

					}

				}

			}
			else if (!leaf.isEntity()) {

				if (!leaf.isWritable()) {
					continue;
				}

				if (null != ppp.getBaseProperty() && ppp.getBaseProperty().isIdProperty()) {
					continue;
				}

				Column column = Column.of(leaf.getRequiredColumnName()
						.getReference(this.entityManager.getDialect().getIdentifierProcessing()));
				Parameter val = Parameter.builder().property(PARAM_INSTANCE_PREFIX + ppp.toDotPath())
						.javaType(leaf.getJavaType()).jdbcType(leaf.getJdbcType()).typeHandler(leaf.getTypeHandler())
						.build();

				if (leaf.isVersionProperty()) {

					query.customSet(column + " = " + column + " + 1");
					query.eq(leaf.getName(), ParamValue.of(PARAM_INSTANCE_PREFIX + ppp.toDotPath(), null, leaf));

					continue;
				}

				query.set(column, ParamValue.of(PARAM_INSTANCE_PREFIX + ppp.toDotPath(), null, leaf));
			}
		}

		this.idCondition(query, byId);

		return query.presupposed(this.entityManager, this.entity,
				selective ? (byId ? UPDATE_SELECTIVE_BY_ID : UPDATE_SELECTIVE)
						: (byId ? UPDATE_BY_ID : ResidentStatementName.UPDATE),
				null, null);
	}

	private void idCondition(ConditionsImpl<?, ?, String, ParamValue> query, boolean byId) {

		MybatisPersistentPropertyImpl idProperty = this.entity.getRequiredIdProperty();
		if (this.entity.isCompositeId()) {
			this.entityManager.findPersistentPropertyPaths(this.entity.getType(), p -> true).stream()
					.filter(ppp -> null != ppp.getBaseProperty() && ppp.getBaseProperty().isIdProperty()
							&& !ppp.getRequiredLeafProperty().isEntity())
					.forEach(ppp -> {
						MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();
						query.eq(ppp.toDotPath(),
								ParamValue.of(byId
										? ("id." + ppp.toDotPath(
												source -> source == ppp.getBaseProperty() ? null : source.getName()))
										: ("instance." + ppp.toDotPath()), null, leaf));
					});
		}
		else {
			query.eq(idProperty.getName(),
					ParamValue.of(byId ? "id" : ("instance." + idProperty.getName()), null, idProperty));
		}
	}

	private void idsCondition(ConditionsImpl<?, ?, String, ParamValue> query, boolean byId) {

		if (!this.entity.hasIdProperty()) {
			return;
		}

		MybatisPersistentPropertyImpl idProperty = this.entity.getRequiredIdProperty();

		if (this.entity.isCompositeId()) {
			query.custom(
					Foreach.builder().collection(byId ? "id" : "instance").separator(") OR (").open("(").close(")")
							.contents(Collections.singletonList(SQL.of(this.entityManager
									.findPersistentPropertyPaths(this.entity.getType(), p -> true).stream()
									.filter(ppp -> null != ppp.getBaseProperty() && ppp.getBaseProperty().isIdProperty()
											&& !ppp.getRequiredLeafProperty().isEntity())
									.map(ppp -> Column
											.base(ppp.getRequiredLeafProperty().getColumnName().getReference(
													this.entityManager.getDialect().getIdentifierProcessing()))
											+ "=" + Parameter.of("item." + (byId ? //
													ppp.toDotPath(source -> source == ppp.getBaseProperty() ? null
															: source.getName())//
													: ppp.toDotPath())))
									.collect(Collectors.joining(" AND ")))))
							.build().toString());
		}
		else {
			if (byId) {
				query.in(idProperty.getName(), ParamValue.of("id", null));
			}
			else {
				query.custom(//
						idProperty.getColumnName()
								.getReference(this.entityManager.getDialect().getIdentifierProcessing())//
								+ " IN " + Choose.of(//
										MethodInvocation.of(Syntax.class, "isEmpty", "instance").toString(), //
										SQL.of("(NULL)"), //
										Foreach.builder().collection("instance")
												.contents(Collections
														.singletonList(Parameter.of(("item." + idProperty.getName()))))
												.build()));
			}
		}
	}

	public SqlDefinition deleteById(boolean byId) {

		DefaultDeleteQuery<?, ParamValue> query = DeleteQuery.create(this.entity.getType());

		this.idCondition(query, byId);
		MybatisPersistentPropertyImpl versionProperty = this.entity.getVersionProperty();
		if (null != versionProperty && !byId) {
			query.and(
					c -> c.eq(versionProperty.getName(), ParamValue.of("instance." + versionProperty.getName(), null)));
		}

		return query.presupposed(this.entityManager, this.entity, byId ? DELETE_BY_ID : DELETE_BY_ENTITY, null, null);
	}

	public SqlDefinition deleteAllInBatch() {

		return DeleteQuery.create(this.entity.getType()).presupposed(this.entityManager, this.entity, DELETE_ALL, null,
				null);
	}

	public SqlDefinition deleteAllByIdInBatch() {

		DefaultDeleteQuery<?, ParamValue> query = DeleteQuery.create(this.entity.getType());
		this.idsCondition(query, true);
		return query.presupposed(this.entityManager, this.entity, DELETE_BY_IDS, null, null);
	}

	public SqlDefinition deleteAllByEntitiesInBatch() {

		DefaultDeleteQuery<?, ParamValue> query = DeleteQuery.create(this.entity.getType());
		this.idsCondition(query, false);
		return query.presupposed(this.entityManager, this.entity, DELETE_BY_ENTITIES, null, null);
	}

	public Select countAll() {

		return CriteriaQuery.create(this.entity.getType()).resultType("long").selects(COUNTS.getValue())
				.presupposed(this.entityManager, this.entity, COUNT_ALL, null, false);
	}

	public Select existsById() {

		if (!this.entity.hasIdProperty()) {
			return null;
		}

		DefaultCriteriaQuery<?, ParamValue> query = CriteriaQuery.create(this.entity.getType());
		query.selects(COUNTS.getValue());
		this.idCondition(query, true);
		return query.resultType("boolean").presupposed(this.entityManager, this.entity, EXISTS_BY_ID, null, false);
	}

	public Select findById() {

		if (!this.entity.hasIdProperty()) {
			return null;
		}

		DefaultCriteriaQuery<?, ParamValue> query = CriteriaQuery.create(this.entity.getType());
		this.idCondition(query, true);
		return query.presupposed(this.entityManager, this.entity, FIND_BY_ID, null, true);
	}

	public Select findByIds() {

		if (!this.entity.hasIdProperty()) {
			return null;
		}

		DefaultCriteriaQuery<?, ParamValue> query = CriteriaQuery.create(this.entity.getType());
		this.idsCondition(query, true);
		return query.presupposed(this.entityManager, this.entity, FIND_BY_IDS, null, true);
	}

	public Select findAll() {

		return CriteriaQuery.create(this.entity.getType()).presupposed(this.entityManager, this.entity, FIND_ALL, null,
				true);
	}

	public Select findAllWithSort() {

		return CriteriaQuery.create(this.entity.getType()).withSort().presupposed(this.entityManager, this.entity,
				FIND_ALL_WITH_SORT, null, true);
	}

	public Select findByPage() {

		return CriteriaQuery.create(this.entity.getType()).paging().presupposed(this.entityManager, this.entity,
				FIND_BY_PAGE, null, true);
	}

	public Select count() {

		return CriteriaQuery.create(this.entity.getType()).resultType("long").selects(COUNTS.getValue())
				.presupposed(this.entityManager, this.entity, COUNT, null, false);
	}

	public Select queryByExample() {

		return CriteriaQuery.create(this.entity.getType()).exampling().presupposed(this.entityManager, this.entity,
				QUERY_BY_EXAMPLE, null, true);
	}

	public Select queryByExampleWithSort() {

		return CriteriaQuery.create(this.entity.getType()).withSort().exampling().presupposed(this.entityManager,
				this.entity, QUERY_BY_EXAMPLE_WITH_SORT, null, true);
	}

	public Select queryByExampleWithPage() {

		return CriteriaQuery.create(this.entity.getType()).paging().exampling().presupposed(this.entityManager,
				this.entity, QUERY_BY_EXAMPLE_WITH_PAGE, null, true);
	}

	public Select countByExample() {

		return CriteriaQuery.create(this.entity.getType()).resultType("long").selects(COUNTS.getValue()).exampling()
				.presupposed(this.entityManager, this.entity, COUNT_QUERY_BY_EXAMPLE, null, true);
	}

	public Select existsByExample() {

		return CriteriaQuery.create(this.entity.getType()).resultType("boolean").selects(COUNTS.getValue()).exampling()
				.presupposed(this.entityManager, this.entity, EXISTS_BY_EXAMPLE, null, true);
	}

	public Select findByCriteria() {
		// try to bootstrap
		return CriteriaQuery.create(this.entity.getType()).binding().presupposed(this.entityManager, this.entity,
				FIND_BY_CRITERIA, null, true);
	}

}
