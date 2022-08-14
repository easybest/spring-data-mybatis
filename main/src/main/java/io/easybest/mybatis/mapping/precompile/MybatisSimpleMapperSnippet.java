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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.SequenceGenerators;

import io.easybest.mybatis.auxiliary.SQLResult;
import io.easybest.mybatis.auxiliary.Syntax;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisAssociation;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.precompile.Insert.SelectKey;
import io.easybest.mybatis.mapping.precompile.ResultMap.Association;
import io.easybest.mybatis.mapping.precompile.ResultMap.ResultMapping;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;
import io.easybest.mybatis.repository.support.ResidentStatementName;
import org.apache.ibatis.mapping.ResultFlag;

import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.util.StringUtils;

import static io.easybest.mybatis.mapping.precompile.Constant.COMMA;
import static io.easybest.mybatis.mapping.precompile.Constant.EQUALS;
import static io.easybest.mybatis.mapping.precompile.Include.COLUMN_LIST;
import static io.easybest.mybatis.mapping.precompile.Include.TABLE_NAME;
import static io.easybest.mybatis.mapping.precompile.Include.TABLE_NAME_PURE;
import static io.easybest.mybatis.mapping.precompile.Insert.SelectKey.Order.AFTER;
import static io.easybest.mybatis.mapping.precompile.Insert.SelectKey.Order.BEFORE;
import static io.easybest.mybatis.mapping.precompile.SQL.COUNTS;
import static io.easybest.mybatis.mapping.precompile.SQL.DELETE_FROM;
import static io.easybest.mybatis.mapping.precompile.SQL.FROM;
import static io.easybest.mybatis.mapping.precompile.SQL.SELECT;
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
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_ID;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_IDS;
import static io.easybest.mybatis.repository.support.ResidentStatementName.FIND_BY_PAGE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.INSERT;
import static io.easybest.mybatis.repository.support.ResidentStatementName.INSERT_SELECTIVE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.LAZY_RESULT_MAP;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_BY_EXAMPLE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_BY_EXAMPLE_FOR_PAGE;
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

	private SelectKey selectKey() {

		if (!this.entity.hasIdProperty() || this.entity.isCompositeId()) {
			return null;
		}

		MybatisPersistentPropertyImpl idProperty = this.entity.getRequiredIdProperty();
		GeneratedValue gv = idProperty.findAnnotation(GeneratedValue.class);
		if (null == gv) {
			return null;
		}

		GenerationType generationType = this.entity.getGenerationType();

		if (generationType == GenerationType.IDENTITY) {

			return SelectKey.builder().keyProperty(PARAM_INSTANCE_PREFIX + idProperty.getName())
					.keyColumn(idProperty.getColumnName()
							.getReference(this.entityManager.getDialect().getIdentifierProcessing()))
					.resultType(idProperty.getJavaType()).order(AFTER).contents(Collections
							.singletonList(SQL.of(this.entityManager.getDialect().getIdentitySelectString())))
					.build();
		}
		else if (generationType == GenerationType.SEQUENCE) {

			String sequenceName = DEFAULT_SEQUENCE_NAME;

			Map<String, String> sequenceGenerators = new HashMap<>();
			Optional.ofNullable(idProperty.findPropertyOrOwnerAnnotation(SequenceGenerators.class))
					.filter(sgs -> sgs.value().length > 0)
					.ifPresent(sgs -> Arrays.stream(sgs.value()).filter(sg -> StringUtils.hasText(sg.sequenceName()))
							.forEach(sg -> sequenceGenerators.put(sg.name(), sg.sequenceName())));
			Optional.ofNullable(idProperty.findPropertyOrOwnerAnnotation(SequenceGenerator.class))
					.filter(sg -> StringUtils.hasText(sg.sequenceName()))
					.ifPresent(sg -> sequenceGenerators.put(sg.name(), sg.sequenceName()));

			String sn = sequenceGenerators.get(gv.generator());
			if (StringUtils.hasText(sn)) {
				sequenceName = sn;
			}

			String sql = this.entityManager.getDialect().getSequenceNextValString(sequenceName);
			return SelectKey.builder().keyProperty(PARAM_INSTANCE_PREFIX + idProperty.getName())
					.keyColumn(idProperty.getColumnName()
							.getReference(this.entityManager.getDialect().getIdentifierProcessing()))
					.resultType(idProperty.getJavaType()).order(BEFORE).contents(Collections.singletonList(SQL.of(sql)))
					.build();
		}

		return null;
	}

	public Insert insert(boolean selective) {

		List<Segment> columns = new LinkedList<>();
		List<Segment> values = new LinkedList<>();

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

						Constant column = Constant.builder()
								.value(jc.getColumnName()
										.getReference(this.entityManager.getDialect().getIdentifierProcessing()))
								.build();
						Parameter val = Parameter.builder().property(PARAM_INSTANCE_PREFIX + ppp.toDotPath() + '.'
								+ jc.getReferencedPropertyPath().toDotPath()).build();

						columns.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1)
								.stripSuffix(selective ? 0 : 1).contents(Arrays.asList(column, COMMA)).build());
						values.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1)
								.stripSuffix(selective ? 0 : 1).contents(Arrays.asList(val, COMMA)).build());

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

				Constant column = Constant.builder().value(leaf.getRequiredColumnName()
						.getReference(this.entityManager.getDialect().getIdentifierProcessing())).build();
				Parameter val = Parameter.builder().property(PARAM_INSTANCE_PREFIX + ppp.toDotPath())
						.javaType(leaf.getJavaType()).jdbcType(leaf.getJdbcType()).typeHandler(leaf.getTypeHandler())
						.build();

				if (leaf.isIdProperty()) {

					columns.add(Composite.builder().contents(Arrays.asList(column, COMMA)).build());
					values.add(Composite.builder().contents(Arrays.asList(val, COMMA)).build());

				}
				else {
					columns.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1).stripSuffix(selective ? 0 : 1)
							.contents(Arrays.asList(column, COMMA)).build());
					values.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1).stripSuffix(selective ? 0 : 1)
							.contents(Arrays.asList(val, COMMA)).build());
				}
			}
		}

		return Insert.builder().id(selective ? INSERT_SELECTIVE : INSERT).selectKey(this.selectKey())
				.contents(Arrays.asList(SQL.INSERT_INTO, TABLE_NAME_PURE, SQL.of("("),
						Trim.builder().suffixOverrides(",").contents(columns).build(), SQL.of(") VALUES ("),
						Trim.builder().suffixOverrides(",").contents(values).build(), SQL.of(")")))
				.build();
	}

	public Update update(boolean selective, boolean byId) {

		if (!this.entity.hasIdProperty()) {
			return null;
		}

		List<Segment> sets = new ArrayList<>();
		List<Segment> conditions = new ArrayList<>();

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
						Parameter val = Parameter.builder().property(PARAM_INSTANCE_PREFIX + ppp.toDotPath() + '.'
								+ jc.getReferencedPropertyPath().toDotPath()).build();

						sets.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1).stripSuffix(selective ? 0 : 1)
								.contents(Arrays.asList(column, EQUALS, val, COMMA)).build());

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

					sets.add(SQL.of(column + "=" + column + "+1,"));

					conditions.add(SQL.of("AND " + column + "=" + val));

					continue;
				}

				sets.add(SafeVars.builder().var(val.getProperty()).stripPrefix(1).stripSuffix(selective ? 0 : 1)
						.contents(Arrays.asList(column, EQUALS, val, COMMA)).build());

			}
		}

		conditions.add(this.idCondition(byId, false));
		conditions.add(this.logicDeleteClause(false));

		return Update.builder()
				.id(selective ? (byId ? UPDATE_SELECTIVE_BY_ID : UPDATE_SELECTIVE)
						: (byId ? UPDATE_BY_ID : ResidentStatementName.UPDATE))
				.contents(Arrays.asList(SQL.UPDATE, TABLE_NAME_PURE, Set.builder().contents(sets).build(),
						Where.builder().contents(conditions).build(),
						SQL.of(this.entityManager.getDialect().limitN(1))))
				.build();
	}

	public SqlDefinition deleteById(boolean byId) {

		List<Segment> conditions = new ArrayList<>();

		MybatisPersistentPropertyImpl versionProperty = this.entity.getVersionProperty();

		if (null != versionProperty && !byId) {

			conditions.add(SQL.of("AND "
					+ Column.of(versionProperty.getColumnName()
							.getReference(this.entityManager.getDialect().getIdentifierProcessing()))
					+ "=" + Parameter.builder().property("instance." + versionProperty.getName()).build()));

			conditions.add(this.idCondition(false, false));
		}
		else {
			conditions.add(this.idCondition(byId, false));
		}

		if (this.entity.getLogicDeleteColumn().isPresent()) {

			conditions.add(this.logicDeleteClause(false));

			Column col = Column.base(this.entity.getLogicDeleteColumn().get());
			return Update.builder().id(byId ? DELETE_BY_ID : DELETE_BY_ENTITY)
					.contents(Arrays.asList(SQL.UPDATE, TABLE_NAME, Set.of(SQL.of(col + " = 1")),
							Where.builder().contents(conditions).build(),
							SQL.of(this.entityManager.getDialect().limitN(1))))
					.build();

		}

		return Delete.builder().id(byId ? DELETE_BY_ID : DELETE_BY_ENTITY)
				.contents(Arrays.asList(DELETE_FROM, TABLE_NAME_PURE, Where.builder().contents(conditions).build(),
						SQL.of(this.entityManager.getDialect().limitN(1))))
				.build();
	}

	public SqlDefinition deleteAllInBatch() {

		if (this.entity.getLogicDeleteColumn().isPresent()) {
			Column col = Column.base(this.entity.getLogicDeleteColumn().get());
			return Update.builder().id(DELETE_ALL).contents(Arrays.asList(SQL.UPDATE, TABLE_NAME,
					Set.of(SQL.of(col + " = 1")), Where.of(this.logicDeleteClause(false)))).build();
		}

		return Delete.builder().id(DELETE_ALL).contents(Arrays.asList(DELETE_FROM, TABLE_NAME_PURE)).build();
	}

	public SqlDefinition deleteAllByIdInBatch() {

		if (this.entity.getLogicDeleteColumn().isPresent()) {

			Column col = Column.base(this.entity.getLogicDeleteColumn().get());
			return Update.builder().id(DELETE_BY_IDS)
					.contents(Arrays.asList(SQL.UPDATE, TABLE_NAME, Set.of(SQL.of(col + " = 1")),
							Where.builder()
									.contents(
											Arrays.asList(this.idsCondition(true, true), this.logicDeleteClause(false)))
									.build()))
					.build();
		}

		return Delete.builder().id(DELETE_BY_IDS)
				.contents(Arrays.asList(DELETE_FROM, TABLE_NAME_PURE, Where.of(this.idsCondition(true, false))))
				.build();
	}

	public SqlDefinition deleteAllByEntitiesInBatch() {

		if (this.entity.getLogicDeleteColumn().isPresent()) {

			Column col = Column.base(this.entity.getLogicDeleteColumn().get());
			return Update.builder().id(DELETE_BY_ENTITIES)
					.contents(Arrays.asList(SQL.UPDATE, TABLE_NAME, Set.of(SQL.of(col + " = 1")),
							Where.builder().contents(
									Arrays.asList(this.idsCondition(false, true), this.logicDeleteClause(false)))
									.build()))
					.build();
		}

		return Delete.builder().id(DELETE_BY_ENTITIES)
				.contents(Arrays.asList(DELETE_FROM, TABLE_NAME_PURE, Where.of(this.idsCondition(false, false))))
				.build();
	}

	public Select countAll() {

		return Select.builder().id(COUNT_ALL).resultType("long")
				.contents(Arrays.asList(SELECT, COUNTS, FROM, TABLE_NAME,
						Where.builder().contents(Collections.singletonList(this.logicDeleteClause(false))).build()))
				.build();

	}

	public Select existsById() {

		if (!this.entity.hasIdProperty()) {
			return null;
		}

		return Select.builder().id(EXISTS_BY_ID).resultType("boolean").contents(Arrays.asList(SELECT, COUNTS, FROM,
				TABLE_NAME, Where.of(this.idCondition(true, false), this.logicDeleteClause(false)))).build();
	}

	private SQL logicDeleteClause(boolean baseAlias) {

		if (!this.entity.getLogicDeleteColumn().isPresent()) {
			return SQL.EMPTY;
		}
		Column col = Column.base(this.entity.getLogicDeleteColumn().get(), baseAlias);
		return SQL.of("AND " + col + " = 0");
	}

	private Segment idCondition(boolean byId, boolean baseAlias) {

		if (!this.entity.hasIdProperty()) {
			return SQL.EMPTY;
		}

		MybatisPersistentPropertyImpl idProperty = this.entity.getRequiredIdProperty();
		if (this.entity.isCompositeId()) {
			return Composite.builder().contents(this.entityManager
					.findPersistentPropertyPaths(this.entity.getType(), p -> true).stream()
					.filter(ppp -> null != ppp.getBaseProperty() && ppp.getBaseProperty().isIdProperty()
							&& !ppp.getRequiredLeafProperty().isEntity())
					.map(ppp -> SQL.of("AND "
							+ Column.base(ppp.getRequiredLeafProperty().getColumnName()
									.getReference(this.entityManager.getDialect().getIdentifierProcessing()), baseAlias)
							+ "="
							+ Parameter.builder()
									.property(byId ? ("id." + ppp.toDotPath(
											source -> source == ppp.getBaseProperty() ? null : source.getName()))
											: ("instance." + ppp.toDotPath()))
									.build()))
					.collect(Collectors.toList())).build();

		}

		return SQL
				.of("AND "
						+ Column.base(idProperty.getColumnName()
								.getReference(this.entityManager.getDialect().getIdentifierProcessing()), baseAlias)
						+ " = "
						+ Parameter.builder().property(byId ? "id" : ("instance." + idProperty.getName()))
								.javaType(idProperty.getJavaType()).jdbcType(idProperty.getJdbcType())
								.typeHandler(idProperty.getTypeHandler()).build());

	}

	public Select findById() {

		if (!this.entity.hasIdProperty()) {
			return null;
		}

		return Select.builder().id(FIND_BY_ID).resultMap(RESULT_MAP).contents(Arrays.asList(SELECT, COLUMN_LIST, FROM,
				TABLE_NAME, Where.of(this.idCondition(true, true), this.logicDeleteClause(true)))).build();
	}

	public Select findByIds() {

		if (!this.entity.hasIdProperty()) {
			return null;
		}

		return Select.builder().id(FIND_BY_IDS).resultMap(RESULT_MAP).contents(Arrays.asList(SELECT, COLUMN_LIST, FROM,
				TABLE_NAME, Where.of(this.idsCondition(true, true), this.logicDeleteClause(true)))).build();
	}

	private Segment idsCondition(boolean byId, boolean baseAlias) {

		if (!this.entity.hasIdProperty()) {
			return SQL.EMPTY;
		}

		MybatisPersistentPropertyImpl idProperty = this.entity.getRequiredIdProperty();
		if (this.entity.isCompositeId()) {

			return Foreach.builder().collection(byId ? "id" : "instance").separator(") OR (").open("(").close(")")
					.contents(Collections.singletonList(SQL.of(this.entityManager
							.findPersistentPropertyPaths(this.entity.getType(), p -> true).stream()
							.filter(ppp -> null != ppp.getBaseProperty() && ppp.getBaseProperty().isIdProperty()
									&& !ppp.getRequiredLeafProperty().isEntity())
							.map(ppp -> Column
									.base(ppp.getRequiredLeafProperty().getColumnName().getReference(
											this.entityManager.getDialect().getIdentifierProcessing()), baseAlias)
									+ "="
									+ Parameter.of("item." + (byId ? (ppp.toDotPath(
											source -> source == ppp.getBaseProperty() ? null : source.getName()))
											: ppp.toDotPath())))
							.collect(Collectors.joining(" AND ")))))
					.build();

		}

		return SQL
				.of("AND "
						+ Column.base(idProperty.getColumnName()
								.getReference(this.entityManager.getDialect().getIdentifierProcessing()), baseAlias)
						+ " IN "
						+ Foreach.builder().collection(byId ? "id" : "instance")
								.contents(Collections
										.singletonList(Parameter.of(byId ? "item" : ("item." + idProperty.getName()))))
								.build());

	}

	public Select findAll() {

		return Select.builder().id(FIND_ALL).resultMap(RESULT_MAP).contents(Arrays.asList(

				Bind.of(SQLResult.PARAM_NAME,
						MethodInvocation.of(Syntax.class, "bind", MYBATIS_DEFAULT_PARAMETER_NAME)),
				SELECT, COLUMN_LIST, FROM, TABLE_NAME,

				Interpolation.of(SQLResult.PARAM_CONNECTOR_NAME),

				this.entity.getLogicDeleteColumn().isPresent() ? Where.of(this.logicDeleteClause(true)) : SQL.EMPTY,
				Interpolation.of(SQLResult.PARAM_SORTING_NAME))).build();
	}

	public Select findByPage() {

		return Select.builder().id(FIND_BY_PAGE).resultMap(RESULT_MAP).contents(Collections.singletonList(Page.of(
				this.entityManager.getDialect(), Parameter.pageOffset(), Parameter.pageSize(),

				Bind.of(SQLResult.PARAM_NAME,
						MethodInvocation.of(Syntax.class, "bind", MYBATIS_DEFAULT_PARAMETER_NAME)),

				SELECT, COLUMN_LIST, FROM, TABLE_NAME, Interpolation.of(SQLResult.PARAM_CONNECTOR_NAME),
				this.entity.getLogicDeleteColumn().isPresent() ? Where.of(this.logicDeleteClause(true)) : SQL.EMPTY,
				Interpolation.of(SQLResult.PARAM_SORTING_NAME)))).build();
	}

	public Select count() {

		return Select.builder().id(COUNT).resultType("long").contents(Arrays.asList(SELECT, COUNTS, FROM, TABLE_NAME,
				this.entity.getLogicDeleteColumn().isPresent() ? Where.of(this.logicDeleteClause(true)) : SQL.EMPTY))
				.build();

	}

	public Select queryByExample() {

		return Select.builder().id(QUERY_BY_EXAMPLE).resultMap(RESULT_MAP).contents(Arrays.asList(
				Bind.of(SQLResult.PARAM_NAME,
						MethodInvocation.of(Syntax.class, "bind", MYBATIS_DEFAULT_PARAMETER_NAME)),
				SELECT, COLUMN_LIST, FROM, TABLE_NAME, Interpolation.of(SQLResult.PARAM_CONNECTOR_NAME),

				Where.of(Interpolation.of(SQLResult.PARAM_CONDITION_NAME),
						this.entity.getLogicDeleteColumn().isPresent() ? this.logicDeleteClause(true) : SQL.EMPTY),

				Interpolation.of(SQLResult.PARAM_SORTING_NAME))).build();
	}

	public Select queryByExampleForPage() {

		Select select = this.queryByExample();

		return Select.builder().id(QUERY_BY_EXAMPLE_FOR_PAGE).resultMap(RESULT_MAP)
				.contents(Collections.singletonList(
						Page.builder().dialect(this.entityManager.getDialect()).offset(Parameter.pageOffset())
								.fetchSize(Parameter.pageSize()).contents(select.contents).build()))
				.build();
	}

	public Select countByExample() {

		return Select.builder().id(COUNT_QUERY_BY_EXAMPLE).resultType("long").contents(Arrays.asList(
				Bind.of(SQLResult.PARAM_NAME,
						MethodInvocation.of(Syntax.class, "bind", MYBATIS_DEFAULT_PARAMETER_NAME)),
				SELECT, COUNTS, FROM, TABLE_NAME, Interpolation.of(SQLResult.PARAM_CONNECTOR_NAME),

				Where.of(Interpolation.of(SQLResult.PARAM_CONDITION_NAME),
						this.entity.getLogicDeleteColumn().isPresent() ? this.logicDeleteClause(true) : SQL.EMPTY)))
				.build();

	}

	public Select existsByExample() {

		return Select.builder().id(EXISTS_BY_EXAMPLE).resultType("boolean").contents(Arrays.asList(
				Bind.of(SQLResult.PARAM_NAME,
						MethodInvocation.of(Syntax.class, "bind", MYBATIS_DEFAULT_PARAMETER_NAME)),
				SELECT, COUNTS, FROM, TABLE_NAME, Interpolation.of(SQLResult.PARAM_CONNECTOR_NAME),

				Where.of(Interpolation.of(SQLResult.PARAM_CONDITION_NAME),
						this.entity.getLogicDeleteColumn().isPresent() ? this.logicDeleteClause(true) : SQL.EMPTY)))
				.build();
	}

}
