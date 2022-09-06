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

package io.easybest.mybatis.repository.query.criteria.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.easybest.mybatis.auxiliary.SQLResult;
import io.easybest.mybatis.auxiliary.Syntax;
import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.MybatisPersistentEntityImpl;
import io.easybest.mybatis.mapping.MybatisPersistentPropertyImpl;
import io.easybest.mybatis.mapping.precompile.Bind;
import io.easybest.mybatis.mapping.precompile.Column;
import io.easybest.mybatis.mapping.precompile.Delete;
import io.easybest.mybatis.mapping.precompile.Include;
import io.easybest.mybatis.mapping.precompile.Interpolation;
import io.easybest.mybatis.mapping.precompile.MethodInvocation;
import io.easybest.mybatis.mapping.precompile.Page;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.Segment;
import io.easybest.mybatis.mapping.precompile.Select;
import io.easybest.mybatis.mapping.precompile.SqlDefinition;
import io.easybest.mybatis.mapping.precompile.Update;
import io.easybest.mybatis.mapping.precompile.Where;
import io.easybest.mybatis.mapping.sql.SqlIdentifier;
import io.easybest.mybatis.repository.query.criteria.ColumnResult;
import io.easybest.mybatis.repository.query.criteria.Conditions;
import io.easybest.mybatis.repository.query.criteria.CriteriaQuery;
import io.easybest.mybatis.repository.query.criteria.ParamValueCallback;
import io.easybest.mybatis.repository.query.criteria.Predicate;
import io.easybest.mybatis.repository.query.criteria.PredicateResult;
import io.easybest.mybatis.repository.query.criteria.SegmentResult;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.util.CollectionUtils;

import static io.easybest.mybatis.mapping.precompile.MybatisMapperSnippet.MYBATIS_DEFAULT_PARAMETER_NAME;
import static io.easybest.mybatis.repository.support.ResidentStatementName.RESULT_MAP;

/**
 * .
 *
 * @author Jarvis Song
 * @param <T> domain type
 * @param <R> return type
 * @param <F> field type
 * @param <V> value type
 */
public class CriteriaQueryImpl<T, R, F, V> extends ConditionsImpl<T, R, F, V> implements CriteriaQuery<R, F, V> {

	private Set<String> selectFields;

	private Set<String> excludeSelectFields;

	private Column[] columns;

	private String selects;

	private Sort sort;

	private boolean withSort;

	private boolean paging;

	public CriteriaQueryImpl(Class<T> domainClass) {
		super(domainClass);
	}

	public Class<T> getDomainClass() {
		return this.domainClass;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected R getReturns() {
		return (R) this;
	}

	@Override
	public R paging() {

		this.paging = true;
		return this.getReturns();
	}

	@SuppressWarnings("unchecked")
	@Override
	public final R select(F... fields) {

		if (null != fields && fields.length > 0) {
			this.selectFields = Arrays.stream(fields).map(Predicate::convertFieldName).collect(Collectors.toSet());
		}

		return this.getReturns();
	}

	@Override
	public R selects(Column... columns) {
		this.columns = columns;
		return this.getReturns();
	}

	@Override
	public R selects(String selects) {

		this.selects = selects;
		return this.getReturns();
	}

	@SuppressWarnings("unchecked")
	@Override
	public final R exclude(F... fields) {

		if (null != fields && fields.length > 0) {
			this.excludeSelectFields = Arrays.stream(fields).map(Predicate::convertFieldName)
					.collect(Collectors.toSet());
		}

		return this.getReturns();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final R orderBy(F... fields) {

		if (null != fields && fields.length > 0) {
			this.sort = Sort.by(Arrays.stream(fields).map(Predicate::convertFieldName).toArray(String[]::new));
		}

		return this.getReturns();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final R orderBy(Sort.Direction direction, F... fields) {

		if (null != fields && fields.length > 0) {
			this.sort = Sort.by(direction,
					Arrays.stream(fields).map(Predicate::convertFieldName).toArray(String[]::new));
		}

		return this.getReturns();
	}

	@Override
	public R orderBy(Sort sort) {

		this.sort = sort;

		return this.getReturns();
	}

	@Override
	public R withSort() {

		this.withSort = true;

		return this.getReturns();
	}

	@Override
	protected Conditions<R, F, V> createConditionsInstance() {

		return new CriteriaQueryImpl<>(this.domainClass);
	}

	private SegmentResult selectColumnsFromFields(EntityManager entityManager, Class<?> domainClass,
			MybatisPersistentEntityImpl<?> entity) {

		if (!CollectionUtils.isEmpty(this.selectFields)) {

			SegmentResult sr = new SegmentResult();

			this.selectFields.stream().filter(field -> {

				if (CollectionUtils.isEmpty(this.excludeSelectFields)) {
					return true;
				}

				return !this.excludeSelectFields.contains(field);

			}).map(field -> this.fieldToColumnName(entityManager, domainClass, field, entity)).forEach(cr -> {

				if (null != cr.getConnectors()) {
					sr.add(cr.getConnectors());
				}

				sr.add(cr.getColumn());
			});

			return sr;
		}

		return null;
	}

	private SegmentResult sorting(EntityManager entityManager, MybatisPersistentEntityImpl<?> entity,
			Class<?> domainClass) {

		return null;
	}

	private ColumnResult fieldToColumnName(EntityManager entityManager, Class<?> domainClass, String field,
			MybatisPersistentEntityImpl<?> entity) {

		PersistentPropertyPath<MybatisPersistentPropertyImpl> ppp = entityManager.getPersistentPropertyPath(field,
				domainClass);
		MybatisPersistentPropertyImpl leaf = ppp.getRequiredLeafProperty();

		if (leaf.isAssociation()) {
			// TODO throw exception?
		}

		SqlIdentifier columnName = leaf.getColumnName();
		SqlIdentifier tableAlias = SqlIdentifier.unquoted(SQL.ROOT_ALIAS.getValue());

		if (!entity.isBasic()) {

			String tablePath = ppp.toDotPath(source -> source.isAssociation() ? source.getName() : null);
			if (null != tablePath) {
				tableAlias = SqlIdentifier.quoted(tablePath);
			}

		}

		Column column = Column.of(tableAlias.toSql(entityManager.getDialect().getIdentifierProcessing()),
				columnName.getReference(entityManager.getDialect().getIdentifierProcessing()));

		Set<String> connectors = Syntax.connectors(entityManager, ppp);

		return new ColumnResult(column, connectors);
	}

	public SqlDefinition presupposedDeleteQuery(EntityManager entityManager, MybatisPersistentEntityImpl<?> entity,
			String id, String parameterType, ParamValueCallback callback) {

		PredicateResult pr = this.toConditionSQL(entityManager, callback, true, false);

		if (entity.getLogicDeleteColumn().isPresent()) {

			Column col = Column.of(entity.getLogicDeleteColumn().get());

			return Update.builder().id(id).parameterType(parameterType).contents(Arrays.asList(//
					SQL.UPDATE, //
					Include.TABLE_NAME_PURE, //
					io.easybest.mybatis.mapping.precompile.Set.of(SQL.of(col + " = 1")), //
					Where.of(//
							null == pr ? SQL.EMPTY : SQL.of(pr.getSql()) //
					))).build();
		}

		return Delete.builder().id(id).parameterType(parameterType).contents(Arrays.asList(//
				SQL.DELETE_FROM, //
				Include.TABLE_NAME_PURE, //
				Where.of(//
						null == pr ? SQL.EMPTY : SQL.of(pr.getSql()) //
				))).build();
	}

	public Select presupposedSelectQuery(EntityManager entityManager, MybatisPersistentEntityImpl<?> entity, String id,
			String resultMap, String resultType, String parameterType, ParamValueCallback callback, boolean alias) {

		Select.Builder builder = Select.builder().id(id);

		if (null != resultMap) {
			builder.resultMap(resultMap);
		}
		else if (null != resultType) {
			builder.resultType(resultType);
		}
		else {
			builder.resultMap(RESULT_MAP);
		}

		if (null != parameterType) {
			builder.parameterType(parameterType);
		}

		Segment selects = null;
		Set<String> connectors = new LinkedHashSet<>();

		if (null != this.selects) {
			selects = SQL.of(this.selects);
		}
		else {
			SegmentResult sr = this.selectColumnsFromFields(entityManager, this.domainClass, entity);

			List<Segment> columns = new ArrayList<>();
			if (null != sr && !CollectionUtils.isEmpty(sr.getSegments())) {
				columns.addAll(sr.getSegments());
			}
			if (null != this.columns && this.columns.length > 0) {
				columns.addAll(Arrays.asList(this.columns));
			}

			if (!columns.isEmpty()) {
				selects = SQL.of(columns.stream().map(Segment::toString).collect(Collectors.joining(",")));
			}

			if (null != sr && !CollectionUtils.isEmpty(sr.getConnectors())) {
				connectors.addAll(sr.getConnectors());
			}
		}

		PredicateResult pr = this.toConditionSQL(entityManager, callback, true, alias);
		if (null != pr && !CollectionUtils.isEmpty(pr.getConnectors())) {
			connectors.addAll(pr.getConnectors());
		}

		boolean bind = this.withSort || this.paging;

		Segment[] segments = new Segment[] {
				bind ? Bind.of(SQLResult.PARAM_NAME,
						MethodInvocation.of(Syntax.class, "bind", MYBATIS_DEFAULT_PARAMETER_NAME)) : SQL.EMPTY, //

				SQL.SELECT, //
				null == selects ? (alias ? Include.COLUMN_LIST : Include.COLUMN_LIST_PURE) : selects, //
				SQL.FROM, //
				alias ? Include.TABLE_NAME : Include.TABLE_NAME_PURE, //
				(CollectionUtils.isEmpty(connectors) ? SQL.EMPTY : SQL.of(String.join(" ", connectors))), //
				bind ? Interpolation.of(SQLResult.PARAM_CONNECTOR_NAME) : SQL.EMPTY, //
				Where.of( //
						null == pr ? SQL.EMPTY : SQL.of(pr.getSql()), //
						this.logicDeleteClause(entity, alias)//
				), //
				bind ? Interpolation.of(SQLResult.PARAM_SORTING_NAME) : SQL.EMPTY };

		builder.contents(
				this.paging ? Collections.singletonList(Page.of(entityManager.getDialect(), Parameter.pageOffset(),
						Parameter.pageSize(), Parameter.pageOffsetEnd(), segments)) : Arrays.asList(segments));

		return builder.build();
	}

}
