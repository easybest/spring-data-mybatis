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

package io.easybest.mybatis.repository.query;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Streamable;

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.Placeholder;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.Select;
import io.easybest.mybatis.mapping.precompile.SqlDefinition;
import io.easybest.mybatis.repository.query.MybatisParameters.MybatisParameter;
import io.easybest.mybatis.repository.query.MybatisQueryExecution.DeleteExecution;
import io.easybest.mybatis.repository.query.MybatisQueryExecution.ExistsExecution;
import io.easybest.mybatis.repository.query.criteria.DefaultCriteriaQuery;
import io.easybest.mybatis.repository.query.criteria.DefaultDeleteQuery;
import io.easybest.mybatis.repository.query.criteria.DeleteQuery;
import io.easybest.mybatis.repository.query.criteria.ParamValue;
import io.easybest.mybatis.repository.query.criteria.impl.CriteriaQueryImpl;
import io.easybest.mybatis.repository.support.MybatisContext;
import io.easybest.mybatis.repository.support.Pageable;
import io.easybest.mybatis.repository.support.ResidentStatementName;

import static io.easybest.mybatis.repository.support.MybatisContext.PARAM_ADDITIONAL_VALUES_PREFIX;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_PREFIX;
import static io.easybest.mybatis.repository.support.ResidentStatementName.UNPAGED_PREFIX;

/**
 * .
 *
 * @author Jarvis Song
 */
public class PartTreeMybatisQuery extends AbstractMybatisQuery {

	private final PartTree tree;

	private final MybatisParameters parameters;

	public PartTreeMybatisQuery(EntityManager entityManager, MybatisQueryMethod method) {

		super(entityManager, method);

		Class<?> domainClass = method.getEntityInformation().getJavaType();
		this.parameters = method.getParameters();
		try {

			this.tree = new PartTree(method.getName(), domainClass);
			validate(this.tree, this.parameters, method.toString());

		}
		catch (Exception ex) {
			throw new IllegalArgumentException(
					String.format("Failed to create query for method %s! %s", method, ex.getMessage()), ex);
		}

	}

	@Override
	protected SqlDefinition doCreateSqlDefinition() {

		if (this.tree.isDelete()) {
			return this.delete();
		}
		if (this.tree.isCountProjection()) {
			return this.count();
		}
		if (this.tree.isExistsProjection()) {
			return this.exists();
		}
		if (this.method.isPageQuery()) {
			return this.page();
		}
		if (this.method.isSliceQuery()) {
			return this.slice();
		}
		if (this.method.isCollectionQuery() || this.method.isStreamQuery()) {
			return this.collection();
		}
		if (this.method.isQueryForEntity()) {
			return this.selectOne();
		}

		return Placeholder.of("UNSUPPORTED PART TREE QUERY FOR " + this.method.getStatementName());

	}

	@Override
	protected void processAdditionalParams(MybatisParametersParameterAccessor accessor, MybatisContext<?, ?> context) {

		Sort sort = this.tree.getSort();
		if (sort.isSorted()) {
			Sort dynamicSort = context.getSort();
			if (null != dynamicSort) {
				context.setSort(sort.and(dynamicSort));
			}
			else {
				context.setSort(sort);
			}
		}

		if (this.tree.isLimiting() && null != this.tree.getMaxResults()) {

			int maxResults = this.tree.getMaxResults();

			Pageable pageable = context.getPageable();
			if (null == pageable || pageable.isUnpaged()) {
				context.setPageable(new Pageable(0, maxResults, 0));
			}
			else {
				long offset = pageable.getOffset();
				if (pageable.getSize() > maxResults && pageable.getOffset() > 0) {
					offset = pageable.getOffset() - (pageable.getSize() - maxResults);
				}
				context.setPageable(new Pageable(pageable.getPage(), pageable.getSize(), offset));
			}

		}

	}

	private SqlDefinition collection() {

		return this.createQueryCreator().createQuery().binding().presupposed(this.entityManager, this.entity,
				this.method.getStatementName(), pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv),
				true);
	}

	private SqlDefinition slice() {

		return this.createQueryCreator().createQuery().binding().paging().presupposed(this.entityManager, this.entity,
				this.method.getStatementName(), pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv),
				true,
				Collections.singletonList(this.createQueryCreator().createQuery().binding().presupposed(
						this.entityManager, this.entity, UNPAGED_PREFIX + this.method.getStatementName(),
						pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv), true)));
	}

	private SqlDefinition page() {

		return this.createQueryCreator().createQuery().binding().paging().presupposed(this.entityManager, this.entity,
				this.method.getStatementName(), pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv),
				true, Arrays.asList(//
						this.createQueryCreator().createQuery().resultMap(null).resultType("long").unpaged()
								.selects(SQL.COUNTS.getValue()).binding().presupposed(this.entityManager, this.entity,
										this.method.getCountStatementName(),
										pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv), true), //
						this.createQueryCreator().createQuery().unpaged().binding().presupposed(this.entityManager,
								this.entity, UNPAGED_PREFIX + this.method.getStatementName(),
								pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv), true))//
		);
	}

	private boolean suitableToResultType() {

		if (this.parameters.hasDynamicProjection()) {
			return false;
		}

		return !(this.entity.getType() == this.method.getReturnedObjectType()
				|| this.entity.getType().isAssignableFrom(this.method.getReturnedObjectType()));
	}

	private PartTreeQueryCreator<DefaultCriteriaQuery<?, ParamValue>> createQueryCreator() {

		ParameterMetadataProvider provider = new ParameterMetadataProvider(this.entityManager,
				this.method.getParameters());
		DefaultCriteriaQuery<?, ParamValue> query = io.easybest.mybatis.repository.query.criteria.CriteriaQuery
				.create(this.entity.getType());
		this.resultMapOrType(query);
		if (this.tree.isLimiting()) {
			query.paging();
		}
		return new PartTreeQueryCreator<>(this.tree, provider, query);

	}

	private SqlDefinition count() {

		return this.createQueryCreator().createQuery().resultType("long").selects(SQL.COUNTS.getValue()).presupposed(
				this.entityManager, this.entity, this.method.getStatementName(),
				pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv), true);
	}

	private SqlDefinition exists() {

		return this.createQueryCreator().createQuery().resultType("boolean").selects(SQL.COUNTS.getValue()).presupposed(
				this.entityManager, this.entity, this.method.getStatementName(),
				pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv), true);
	}

	private void resultMapOrType(CriteriaQueryImpl<?, ?, String, ParamValue> query) {

		if (this.method.getResultMap().isPresent()) {
			query.resultMap(this.method.getResultMap().orElse(ResidentStatementName.RESULT_MAP));
		}
		else if (this.method.getResultType().isPresent()) {
			query.resultType(this.method.getActualResultType());
		}
		else {

			if (this.suitableToResultType()) {
				query.resultType(this.method.getActualResultType());
				return;
			}

			query.resultMap(ResidentStatementName.RESULT_MAP);

		}
	}

	private SqlDefinition delete() {

		ParameterMetadataProvider provider = new ParameterMetadataProvider(this.entityManager,
				this.method.getParameters());
		DefaultDeleteQuery<?, ParamValue> query = DeleteQuery.create(this.entity.getType());
		PartTreeQueryCreator<? extends DefaultDeleteQuery<?, ParamValue>> creator = new PartTreeQueryCreator<>(
				this.tree, provider, query);

		Select derived = null;
		if (this.method.isCollectionQuery()) {
			ParameterMetadataProvider criteriaProvider = new ParameterMetadataProvider(this.entityManager,
					this.method.getParameters());
			DefaultCriteriaQuery<?, ParamValue> criteriaQuery = io.easybest.mybatis.repository.query.criteria.CriteriaQuery
					.create(this.entity.getType());
			PartTreeQueryCreator<? extends DefaultCriteriaQuery<?, ParamValue>> queryCreator = new PartTreeQueryCreator<>(
					this.tree, criteriaProvider, criteriaQuery);
			this.resultMapOrType(criteriaQuery);

			derived = queryCreator.createQuery().presupposed(this.entityManager, this.entity,
					QUERY_PREFIX + this.method.getStatementName(),
					pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv), true);
		}

		return creator.createQuery().presupposed(this.entityManager, this.entity, this.method.getStatementName(), null,
				pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv),
				derived == null ? null : Collections.singletonList(derived));
	}

	private SqlDefinition selectOne() {

		return this.createQueryCreator().createQuery().presupposed(this.entityManager, this.entity,
				this.method.getStatementName(), pv -> Parameter.of(PARAM_ADDITIONAL_VALUES_PREFIX + pv.getName(), pv),
				true);
	}

	@Override
	protected MybatisQueryExecution getExecution() {

		if (this.tree.isDelete()) {
			return new DeleteExecution();
		}

		if (this.tree.isExistsProjection()) {
			return new ExistsExecution();
		}

		return super.getExecution();
	}

	public PartTree getTree() {
		return this.tree;
	}

	public MybatisParameters getParameters() {
		return this.parameters;
	}

	private static void validate(PartTree tree, MybatisParameters parameters, String methodName) {

		int argCount = 0;

		@SuppressWarnings({ "rawtypes" })
		Iterable<Part> parts = () -> tree.stream().flatMap(Streamable::stream).iterator();

		for (Part part : parts) {

			int numberOfArguments = part.getNumberOfArguments();

			for (int i = 0; i < numberOfArguments; i++) {

				throwExceptionOnArgumentMismatch(methodName, part, parameters, argCount);

				argCount++;
			}
		}
	}

	private static void throwExceptionOnArgumentMismatch(String methodName, Part part, MybatisParameters parameters,
			int index) {

		Part.Type type = part.getType();
		String property = part.getProperty().toDotPath();

		if (!parameters.getBindableParameters().hasParameterAt(index)) {
			throw new IllegalStateException(String.format(
					"Method %s expects at least %d arguments but only found %d. This leaves an operator of type %s for property %s unbound.",
					methodName, index + 1, index, type.name(), property));
		}

		MybatisParameter parameter = parameters.getBindableParameter(index);

		if (expectsCollection(type) && !parameterIsCollectionLike(parameter)) {
			throw new IllegalStateException(
					wrongParameterTypeMessage(methodName, property, type, "Collection", parameter));
		}
		else if (!expectsCollection(type) && !parameterIsScalarLike(parameter)) {
			throw new IllegalStateException(wrongParameterTypeMessage(methodName, property, type, "scalar", parameter));
		}
	}

	private static String wrongParameterTypeMessage(String methodName, String property, Part.Type operatorType,
			String expectedArgumentType, MybatisParameter parameter) {

		return String.format("Operator %s on %s requires a %s argument, found %s in method %s.", operatorType.name(),
				property, expectedArgumentType, parameter.getType(), methodName);
	}

	private static boolean parameterIsCollectionLike(MybatisParameter parameter) {
		return Iterable.class.isAssignableFrom(parameter.getType()) || parameter.getType().isArray();
	}

	private static boolean parameterIsScalarLike(MybatisParameter parameter) {
		return !Iterable.class.isAssignableFrom(parameter.getType());
	}

	private static boolean expectsCollection(Part.Type type) {
		return type == Part.Type.IN || type == Part.Type.NOT_IN;
	}

}
