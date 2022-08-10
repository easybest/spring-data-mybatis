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

import io.easybest.mybatis.mapping.EntityManager;
import io.easybest.mybatis.mapping.precompile.CriteriaQuery;
import io.easybest.mybatis.mapping.precompile.Delete;
import io.easybest.mybatis.mapping.precompile.Page;
import io.easybest.mybatis.mapping.precompile.Parameter;
import io.easybest.mybatis.mapping.precompile.Placeholder;
import io.easybest.mybatis.mapping.precompile.SQL;
import io.easybest.mybatis.mapping.precompile.Select;
import io.easybest.mybatis.mapping.precompile.SqlDefinition;
import io.easybest.mybatis.repository.query.MybatisParameters.MybatisParameter;
import io.easybest.mybatis.repository.query.MybatisQueryExecution.DeleteExecution;
import io.easybest.mybatis.repository.query.MybatisQueryExecution.ExistsExecution;
import io.easybest.mybatis.repository.support.MybatisContext;
import io.easybest.mybatis.repository.support.Pageable;
import io.easybest.mybatis.repository.support.ResidentStatementName;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Streamable;

import static io.easybest.mybatis.mapping.precompile.CriteriaQuery.Type.COUNT;
import static io.easybest.mybatis.mapping.precompile.CriteriaQuery.Type.DELETE;
import static io.easybest.mybatis.repository.support.ResidentStatementName.QUERY_PREFIX;

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

	private MybatisQueryCreator createCreator() {

		ResultProcessor processor = this.method.getResultProcessor();
		ParameterMetadataProvider provider = new ParameterMetadataProvider(this.entityManager,
				this.method.getParameters());
		ReturnedType returnedType = processor.getReturnedType();
		return new MybatisQueryCreator(this.tree, returnedType, provider, this.entityManager, this.entity);

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

		if (this.tree.isLimiting()) {

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

		MybatisQueryCreator creator = this.createCreator();
		CriteriaQuery query = creator.createQuery().bind(true).columnAsType(this.suitableToResultType());

		SQL sql = SQL.of(query.toString());

		Select.SelectBuilder<?, ?> builder = Select.builder().id(this.method.getStatementName());
		this.resultMapOrType(builder);

		builder.contents(Collections.singletonList(sql));
		return builder.build();
	}

	private SqlDefinition slice() {

		MybatisQueryCreator creator = this.createCreator();
		CriteriaQuery query = creator.createQuery().bind(true).columnAsType(this.suitableToResultType());

		SQL sql = SQL.of(query.toString());

		Select.SelectBuilder<?, ?> builder = Select.builder().id(this.method.getStatementName());
		this.resultMapOrType(builder);

		builder.contents(Collections.singletonList(

				Page.of(this.entityManager.getDialect(), Parameter.pageOffset(), Parameter.pageSize(), sql)));

		Select.SelectBuilder<?, ?> unpagedBuilder = Select.builder()
				.id(ResidentStatementName.UNPAGED_PREFIX + this.method.getStatementName())
				.contents(Collections.singletonList(sql));
		this.resultMapOrType(unpagedBuilder);

		builder.derived(Collections.singletonList(unpagedBuilder.build()));

		return builder.build();
	}

	private SqlDefinition page() {

		MybatisQueryCreator creator = this.createCreator();
		CriteriaQuery query = creator.createQuery().bind(true).columnAsType(this.suitableToResultType());

		SQL sql = SQL.of(query.toString());

		Select.SelectBuilder<?, ?> builder = Select.builder().id(this.method.getStatementName());
		this.resultMapOrType(builder);

		builder.contents(Collections.singletonList(

				Page.of(this.entityManager.getDialect(), Parameter.pageOffset(), Parameter.pageSize(), sql)));

		Select count = Select.builder().id(this.method.getCountStatementName()).resultType("long")
				.contents(Collections.singletonList(SQL.of(query.toString(COUNT)))).build();

		Select.SelectBuilder<?, ?> unpagedBuilder = Select.builder()
				.id(ResidentStatementName.UNPAGED_PREFIX + this.method.getStatementName())
				.contents(Collections.singletonList(sql));
		this.resultMapOrType(unpagedBuilder);

		builder.derived(Arrays.asList(count, unpagedBuilder.build()));

		return builder.build();
	}

	private void resultMapOrType(Select.SelectBuilder<?, ?> builder) {

		if (this.method.getResultMap().isPresent()) {
			builder.resultMap(this.method.getResultMap().orElse(ResidentStatementName.RESULT_MAP));
		}
		else if (this.method.getResultType().isPresent()) {
			builder.resultType(this.method.getActualResultType());
		}
		else {

			if (this.suitableToResultType()) {
				builder.resultType(this.method.getActualResultType());
				return;
			}

			builder.resultMap(ResidentStatementName.RESULT_MAP);

		}
	}

	private boolean suitableToResultType() {

		return !(this.entity.getType() == this.method.getReturnedObjectType()
				|| this.entity.getType().isAssignableFrom(this.method.getReturnedObjectType()));
	}

	private SqlDefinition exists() {

		MybatisQueryCreator creator = this.createCreator();
		CriteriaQuery query = creator.createQuery().select(SQL.COUNTS);

		return Select.builder().id(this.method.getStatementName()).resultType("boolean")
				.contents(Collections.singletonList(

						SQL.of(query.toString())

				)).build();
	}

	private SqlDefinition count() {

		MybatisQueryCreator creator = this.createCreator();
		CriteriaQuery query = creator.createQuery().select(SQL.COUNTS);

		return Select.builder().id(this.method.getStatementName()).resultType("long")
				.contents(Collections.singletonList(

						SQL.of(query.toString())

				)).build();

	}

	private SqlDefinition delete() {

		if (this.entity.getLogicDeleteColumn().isPresent()) {
			// TODO

		}

		MybatisQueryCreator creator = this.createCreator();

		Delete.DeleteBuilder<?, ?> builder = Delete.builder().id(this.method.getStatementName());

		CriteriaQuery cq = creator.createQuery();

		if (this.method.isCollectionQuery()) {

			Select.SelectBuilder<?, ?> selectBuilder = Select.builder()
					.id(QUERY_PREFIX + this.method.getStatementName());

			this.resultMapOrType(selectBuilder);

			selectBuilder.contents(Collections.singletonList(SQL.of(cq.toString())));
			Select select = selectBuilder.build();
			builder.derived(Collections.singletonList(select));
		}

		builder.contents(Collections.singletonList(SQL.of(cq.toString(DELETE))));
		return builder.build();
	}

	private SqlDefinition selectOne() {

		Select.SelectBuilder<?, ?> builder = Select.builder().id(this.method.getStatementName());
		this.resultMapOrType(builder);

		MybatisQueryCreator creator = this.createCreator();
		CriteriaQuery query = creator.createQuery().bind(true).columnAsType(this.suitableToResultType());
		SQL sql = SQL.of(query.toString());

		builder.contents(Collections.singletonList(this.tree.isLimiting()
				? Page.of(this.entityManager.getDialect(), Parameter.pageOffset(), Parameter.pageSize(), sql) : sql));
		return builder.build();
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
