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
package org.springframework.data.mybatis.repository.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EmbeddedId;

import com.samskivert.mustache.Mustache.Lambda;
import lombok.Getter;
import org.apache.ibatis.session.Configuration;

import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;
import org.springframework.data.mybatis.dialect.pagination.SQLServer2005LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.SQLServer2012LimitHandler;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentEntity;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.mapping.model.Column;
import org.springframework.data.mybatis.repository.query.MybatisParameters.MybatisParameter;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.StringUtils;

/**
 * Part tree query precompiler.
 *
 * @author JARVIS SONG
 */
class PartTreeMyBatisQueryPrecompiler extends MybatisQueryMethodPrecompiler {

	private final PartTreeMybatisQuery query;

	private AtomicInteger argumentCounter = new AtomicInteger(0);

	PartTreeMyBatisQueryPrecompiler(MybatisMappingContext mappingContext, Configuration configuration, Dialect dialect,
			PartTreeMybatisQuery query) {
		super(mappingContext, configuration, dialect, query);

		this.query = query;
	}

	@Override
	protected String mainQueryString() {
		return null;
	}

	@Override
	protected String doPrecompile() {
		PartTree tree = this.query.getTree();
		MybatisQueryMethod method = this.query.getQueryMethod();

		if (tree.isDelete()) {
			return this.addDeleteStatement(tree, method);
		}
		if (tree.isCountProjection()) {
			return this.addCountStatement();
		}
		if (tree.isExistsProjection()) {
			return this.addExistsStatement();
		}
		if (method.isPageQuery()) {
			return this.addPageStatement(true);
		}
		if (method.isSliceQuery()) {
			return this.addPageStatement(false);
		}
		if (method.isCollectionQuery() || method.isStreamQuery()) {
			return this.addCollectionStatement();
		}

		if (method.isQueryForEntity()) {
			return this.buildSelectStatementSegment(this.query.getStatementName(), false);
		}

		return null;
	}

	private String addDeleteStatement(PartTree tree, MybatisQueryMethod method) {

		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", this.query.getStatementName());
		scopes.put("table", this.getTableName());
		scopes.put("tree", this.convert(tree));

		if (method.isCollectionQuery()) {
			// need to return the deleted entities collection
			return this.render("PartTreeDelete", scopes) + this.buildSelectStatementSegment(
					ResidentStatementName.QUERY_PREFIX + this.query.getStatementName(), false);
		}

		return this.render("PartTreeDelete", scopes);

	}

	private String addCountStatement() {
		return this.buildCountStatementSegment(this.query.getStatementName());
	}

	private String addExistsStatement() {
		return this.buildCountStatementSegment(this.query.getStatementName());
	}

	private String addPageStatement(boolean includeCount) {
		String sql = this.buildSelectStatementSegment(this.query.getStatementName(), true);
		sql += this.buildSelectStatementSegment(ResidentStatementName.UNPAGED_PREFIX + this.query.getStatementName(),
				false);
		if (includeCount) {
			String count = this.buildCountStatementSegment(this.query.getCountStatementName());
			return sql + count;
		}
		return sql;
	}

	private String addCollectionStatement() {
		return this.buildSelectStatementSegment(this.query.getStatementName(),
				this.query.getQueryMethod().getParameters().hasPageableParameter());
	}

	private String buildCountStatementSegment(String statementName) {
		PartTree tree = this.query.getTree();
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", statementName);
		scopes.put("table", this.getTableName());
		scopes.put("tree", this.convert(tree));
		scopes.put("columns", tree.isLimiting() ? "1" : "COUNT(*)");
		scopes.put("limiting", tree.isLimiting());
		if (tree.isLimiting()) {
			RowSelection selection = new RowSelection(0, tree.getMaxResults());
			scopes.put("limitHandler", (Lambda) (frag, out) -> {
				out.write(this.dialect.getLimitHandler().processSql(frag.execute(), selection));
			});
			scopes.put("SQLServer2005", this.dialect.getLimitHandler().getClass() == SQLServer2005LimitHandler.class);
			scopes.put("SQLServer2012", this.dialect.getLimitHandler().getClass() == SQLServer2012LimitHandler.class);
		}

		return this.render("PartTreeCount", scopes);
	}

	private String buildSelectStatementSegment(String statementName, boolean pageable) {
		PartTree tree = this.query.getTree();
		MybatisQueryMethod method = this.query.getQueryMethod();
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("statementName", statementName);
		scopes.put("table", this.getTableName());
		scopes.put("tree", this.convert(tree));

		String columns = "*";
		if (StringUtils.hasText(method.getSelectColumns())) {
			String[] selectColumns = method.getSelectColumns().split(",");
			columns = Stream.of(selectColumns).map(String::trim).filter(StringUtils::hasText).map(
					sc -> this.persistentEntity.getRequiredPersistentProperty(sc).getColumn().getName().render(dialect))
					.collect(Collectors.joining(","));
		}
		scopes.put("columns", columns);
		if (null != tree.getSort() || method.getParameters().hasSortParameter()) {
			scopes.put("sortable", true);
		}
		if (tree.isDistinct()) {
			scopes.put("distinct", true);
		}
		if (StringUtils.hasText(method.getResultMap())) {
			scopes.put("isResultMap", true);
			scopes.put("result", method.getResultMap());
		}
		else if (null != method.getResultType()) {
			scopes.put("isResultMap", false);
			if (method.getResultType() == Void.class) {
				scopes.put("result", method.getActualResultType());
			}
			else {
				scopes.put("result", method.getResultType().getName());
			}
		}
		else {
			scopes.put("isResultMap", true);
			scopes.put("result", ResidentStatementName.RESULT_MAP);
		}

		RowSelection selection = null;
		if (tree.isLimiting()) {
			if (!pageable) {
				selection = new RowSelection(0, tree.getMaxResults());
			}
			pageable = true;
		}
		if (pageable) {
			if (null == selection) {
				selection = new RowSelection(true);
			}

			RowSelection rowSelection = selection;
			scopes.put("limitHandler", (Lambda) (frag, out) -> {
				out.write(this.dialect.getLimitHandler().processSql(frag.execute(), rowSelection));
			});
			scopes.put("SQLServer2005", this.dialect.getLimitHandler().getClass() == SQLServer2005LimitHandler.class);
			scopes.put("SQLServer2012", this.dialect.getLimitHandler().getClass() == SQLServer2012LimitHandler.class);
		}

		scopes.put("pageable", pageable);
		return render("PartTreeSelect", scopes);
	}

	public List<OrPart> convert(PartTree tree) {
		this.argumentCounter.set(0);
		return tree.stream().map(orPart -> new OrPart(orPart, this.persistentEntity, this.mappingContext,
				this.argumentCounter, this.query.getQueryMethod(), this.dialect)).collect(Collectors.toList());
	}

	@Override
	protected String getResourceSuffix() {
		return "_tree_" + this.query.getStatementName() + super.getResourceSuffix();
	}

	@Getter
	public static class OrPart {

		private List<AndPart> parts;

		OrPart(PartTree.OrPart or, MybatisPersistentEntity<?> persistentEntity, MybatisMappingContext mappingContext,
				AtomicInteger argumentCounter, MybatisQueryMethod method, Dialect dialect) {
			this.parts = or.stream()
					.map(part -> new AndPart(part, persistentEntity, mappingContext, argumentCounter, method, dialect))
					.collect(Collectors.toList());
		}

	}

	@Getter
	public static class AndPart {

		private final Column column;

		private final String[] arguments;

		private boolean ignoreCase;

		private String lowercaseFunction;

		private boolean opBetween;

		private boolean opNotNull;

		private boolean opNull;

		private boolean opRlike;

		private boolean opLlike;

		private boolean opLike;

		private boolean opIn;

		private boolean opNotIn;

		private boolean opTrue;

		private boolean opFalse;

		private boolean opDefault;

		private boolean opLessThan;

		private boolean opLessThanEqual;

		private boolean opGreaterThan;

		private boolean opGreaterThanEqual;

		private boolean opBefore;

		private boolean opAfter;

		private boolean opNotLike;

		private boolean opIsEmpty;

		private boolean opIsNotEmpty;

		private boolean opNear;

		private boolean opWithin;

		private boolean opRegex;

		private boolean opExists;

		private boolean opNegatingSimpleProperty;

		private boolean opSimpleProperty;

		private boolean arrayParameter;

		AndPart(Part part, MybatisPersistentEntity<?> persistentEntity, MybatisMappingContext mappingContext,
				AtomicInteger argumentCounter, MybatisQueryMethod method, Dialect dialect) {
			MybatisPersistentProperty persistentProperty = persistentEntity
					.getRequiredPersistentProperty(part.getProperty().getSegment());
			if (persistentProperty.isAnnotationPresent(EmbeddedId.class) || persistentProperty.isEmbeddable()) {
				MybatisPersistentEntity<?> leafEntity = mappingContext
						.getRequiredPersistentEntity(part.getProperty().getLeafProperty().getOwningType());
				persistentProperty = leafEntity
						.getPersistentProperty(part.getProperty().getLeafProperty().getSegment());
			}
			if (part.shouldIgnoreCase() == IgnoreCaseType.ALWAYS
					|| (part.shouldIgnoreCase() == IgnoreCaseType.WHEN_POSSIBLE && null != persistentProperty
							&& CharSequence.class.isAssignableFrom(persistentProperty.getType()))) {
				this.ignoreCase = true;
				this.lowercaseFunction = dialect.getLowercaseFunction();
			}
			this.column = persistentProperty.getColumn();

			this.arguments = new String[part.getNumberOfArguments()];
			if (part.getNumberOfArguments() > 0) {
				MybatisParameters parameters = method.getParameters();
				for (int i = 0; i < part.getNumberOfArguments(); i++) {
					int counter = argumentCounter.getAndIncrement();
					MybatisParameter bindableParameter = parameters.getBindableParameter(counter);
					this.arguments[i] = bindableParameter.getName().orElse("__p" + (bindableParameter.getIndex() + 1));
					this.arrayParameter = bindableParameter.getType().isArray();
				}
			}

			switch (part.getType()) {
			case BETWEEN:
				this.opBetween = true;
				break;
			case IS_NOT_NULL:
				this.opNotNull = true;
				break;
			case IS_NULL:
				this.opNull = true;
				break;
			case STARTING_WITH:
				this.opRlike = true;
				break;
			case ENDING_WITH:
				this.opLlike = true;
				break;
			case LESS_THAN:
				this.opDefault = true;
				this.opLessThan = true;
				break;
			case LESS_THAN_EQUAL:
				this.opDefault = true;
				this.opLessThanEqual = true;
				break;
			case GREATER_THAN:
				this.opDefault = true;
				this.opGreaterThan = true;
				break;
			case GREATER_THAN_EQUAL:
				this.opDefault = true;
				this.opGreaterThanEqual = true;
				break;
			case BEFORE:
				this.opDefault = true;
				this.opBefore = true;
				break;
			case AFTER:
				this.opDefault = true;
				this.opAfter = true;
				break;
			case NOT_LIKE:
			case NOT_CONTAINING:
				this.opLike = true;
				this.opNotLike = true;
				break;
			case CONTAINING:
			case LIKE:
				this.opLike = true;
				break;
			case NOT_IN:
				this.opNotIn = true;
				break;
			case IN:
				this.opIn = true;
				break;
			case TRUE:
				this.opTrue = true;
				break;
			case FALSE:
				this.opFalse = true;
				break;
			case IS_NOT_EMPTY:
				this.opIsNotEmpty = true;
				break;
			case IS_EMPTY:
				this.opIsEmpty = true;
				break;
			case NEAR:
				this.opNear = true;
				break;
			case WITHIN:
				this.opWithin = true;
				break;
			case REGEX:
				this.opRegex = true;
				break;
			case EXISTS:
				this.opExists = true;
				break;
			case NEGATING_SIMPLE_PROPERTY:
				this.opDefault = true;
				this.opNegatingSimpleProperty = true;
				break;
			case SIMPLE_PROPERTY:
				this.opSimpleProperty = true;
				this.opDefault = true;
				break;
			default:
				this.opDefault = true;
				break;
			}
		}

	}

}
