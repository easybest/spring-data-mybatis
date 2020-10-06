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
package org.springframework.data.mybatis.precompiler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.samskivert.mustache.Mustache.Lambda;
import lombok.Getter;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;
import org.springframework.data.mybatis.dialect.pagination.SQLServer2005LimitHandler;
import org.springframework.data.mybatis.dialect.pagination.SQLServer2012LimitHandler;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.model.Column;
import org.springframework.data.mybatis.repository.query.MybatisParameters;
import org.springframework.data.mybatis.repository.query.MybatisParameters.MybatisParameter;
import org.springframework.data.mybatis.repository.query.MybatisQueryMethod;
import org.springframework.data.mybatis.repository.query.PartTreeMybatisQuery;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.IgnoreCaseType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Streamable;
import org.springframework.util.StringUtils;

/**
 * Part tree query precompiler.
 *
 * @author JARVIS SONG
 */
class PartTreeMybatisQueryPrecompiler extends AbstractMybatisPrecompiler {

	private final PartTreeMybatisQuery query;

	private final AtomicInteger argumentCounter = new AtomicInteger(0);

	public PartTreeMybatisQueryPrecompiler(MybatisMappingContext mappingContext, PartTreeMybatisQuery query) {
		super(mappingContext,
				mappingContext.getRequiredModel(query.getQueryMethod().getEntityInformation().getJavaType()));
		this.query = query;
	}

	@Override
	protected Collection<String> prepareStatements() {

		String statement = this.doPrepareStatement();
		if (StringUtils.isEmpty(statement)) {
			return Collections.emptyList();
		}
		return Collections.singletonList(statement);
	}

	@Override
	protected String getResource(String dir, String namespace) {
		return "tree/" + this.query.getStatementId() + ".xml";
	}

	private String doPrepareStatement() {
		PartTree tree = this.query.getTree();
		MybatisQueryMethod method = this.query.getQueryMethod();
		if (tree.isDelete()) {
			return this.deleteStatement();
		}
		if (tree.isCountProjection()) {
			return this.countProjectionStatement();
		}
		if (tree.isExistsProjection()) {
			return this.existsProjectionStatement();
		}
		if (method.isPageQuery()) {
			return this.pageQueryStatement();
		}
		if (method.isSliceQuery()) {
			return this.sliceQueryStatement();
		}
		if (method.isCollectionQuery() || method.isStreamQuery()) {
			return this.collectionQueryStatement();
		}
		if (method.isQueryForEntity()) {
			return this.selectQueryStatement(false);
		}
		return null;
	}

	private String deleteStatement() {
		return this.render("PartTreeDelete", null);
	}

	private String countProjectionStatement() {
		PartTree tree = this.query.getTree();
		Map<String, Object> scopes = new HashMap<>();
		scopes.put("limiting", tree.isLimiting());
		scopes.put("columns", tree.isLimiting() ? "1" : "COUNT(*)");
		if (tree.isLimiting()) {
			RowSelection selection = new RowSelection(0, tree.getMaxResults());
			scopes.put("limitHandler", (Lambda) (frag, out) -> {
				out.write(this.mappingContext.getDialect().getLimitHandler().processSql(frag.execute(), selection));
			});
			scopes.put("SQLServer2005",
					this.mappingContext.getDialect().getLimitHandler().getClass() == SQLServer2005LimitHandler.class);
			scopes.put("SQLServer2012",
					this.mappingContext.getDialect().getLimitHandler().getClass() == SQLServer2012LimitHandler.class);
		}
		return this.render("PartTreeCount", scopes);
	}

	private String existsProjectionStatement() {
		return this.countProjectionStatement();
	}

	private String pageQueryStatement() {
		Map<String, Object> scopes = new HashMap<>();
		return this.render("PartTreePage", scopes);
	}

	private String sliceQueryStatement() {
		Map<String, Object> scopes = new HashMap<>();
		return this.render("PartTreeSlice", scopes);
	}

	private String collectionQueryStatement() {
		// Map<String, Object> scopes = new HashMap<>();
		// return this.render("PartTreeCollection", scopes);
		return this.selectQueryStatement(this.query.getQueryMethod().getParameters().hasPageableParameter());
	}

	private String selectQueryStatement(boolean pageable) {
		PartTree tree = this.query.getTree();
		MybatisQueryMethod method = this.query.getQueryMethod();
		Map<String, Object> scopes = new HashMap<>();
		String columns = "<include refid=\"__column_list\"/>";
		if (StringUtils.hasText(method.getSelectColumns())) {
			String[] selectedColumns = method.getSelectColumns().split(",");
			columns = Arrays.stream(selectedColumns).map(String::trim).filter(StringUtils::hasText).map(
					sc -> this.domain.findColumnByPropertyName(sc).getName().render(this.mappingContext.getDialect()))
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
			if (method.getResultType() == Void.class || method.getResultType() == void.class) {
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
			scopes.put("limitHandler", (Lambda) (frag, out) -> out.write(
					this.mappingContext.getDialect().getLimitHandler().processSql(frag.execute(), rowSelection)));
			scopes.put("SQLServer2005",
					this.mappingContext.getDialect().getLimitHandler().getClass() == SQLServer2005LimitHandler.class);
			scopes.put("SQLServer2012",
					this.mappingContext.getDialect().getLimitHandler().getClass() == SQLServer2012LimitHandler.class);
		}
		scopes.put("pageable", pageable);

		return this.render("PartTreeSelect", scopes);
	}

	@Override
	protected String render(String name, Map<String, Object> scopes) {
		if (null == scopes) {
			scopes = new HashMap<>();
		}
		scopes.putIfAbsent(SCOPE_STATEMENT_NAME, this.query.getStatementName());
		scopes.put("tree", this.convert(this.query.getTree()));
		scopes.put("lowercaseFunction", this.mappingContext.getDialect().getLowercaseFunction());
		return super.render(name, scopes);
	}

	private List<OrPart> convert(PartTree tree) {
		this.argumentCounter.set(0); // reset the argument counter
		return tree.stream().map(orPart -> new OrPart(orPart)).collect(Collectors.toList());
	}

	public class OrPart implements Streamable<AndPart> {

		private final List<AndPart> andParts;

		public OrPart(PartTree.OrPart orPart) {
			this.andParts = orPart.stream().map(part -> new AndPart(part)).collect(Collectors.toList());
		}

		@Override
		public Iterator<AndPart> iterator() {
			return this.andParts.iterator();
		}

	}

	@Getter
	public class AndPart {

		private final Column column;

		private final String[] arguments;

		private boolean ignoreCase;

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

		public AndPart(Part part) {
			PropertyPath propertyPath = part.getProperty();
			this.column = PartTreeMybatisQueryPrecompiler.this.domain.findColumn(propertyPath);
			if (null == this.column) {
				throw new MappingException("Could not find column for " + propertyPath);
			}

			if (part.shouldIgnoreCase() == IgnoreCaseType.ALWAYS
					|| (part.shouldIgnoreCase() == IgnoreCaseType.WHEN_POSSIBLE && this.column.isString())) {
				this.ignoreCase = true;
			}

			this.arguments = new String[part.getNumberOfArguments()];
			if (part.getNumberOfArguments() > 0) {
				MybatisParameters parameters = PartTreeMybatisQueryPrecompiler.this.query.getQueryMethod()
						.getParameters();
				for (int i = 0; i < part.getNumberOfArguments(); i++) {
					int counter = PartTreeMybatisQueryPrecompiler.this.argumentCounter.getAndIncrement();
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
