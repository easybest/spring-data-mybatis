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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static io.easybest.mybatis.repository.query.JSqlParserUtils.getJSqlCount;
import static io.easybest.mybatis.repository.query.JSqlParserUtils.getJSqlLower;
import static io.easybest.mybatis.repository.query.QueryUtils.checkSortExpression;

/**
 * .
 *
 * @author Jarvis Song
 */
public class JSqlParserQueryEnhancer implements QueryEnhancer {

	private final DeclaredQuery query;

	private final ParsedType parsedType;

	public JSqlParserQueryEnhancer(DeclaredQuery query) {

		this.query = query;
		this.parsedType = this.detectParsedType();
	}

	private ParsedType detectParsedType() {

		try {
			Statement statement = CCJSqlParserUtil.parse(this.query.getQueryString());

			if (statement instanceof Update) {
				return ParsedType.UPDATE;
			}
			else if (statement instanceof Delete) {
				return ParsedType.DELETE;
			}
			else if (statement instanceof Select) {
				return ParsedType.SELECT;
			}
			else {
				return ParsedType.SELECT;
			}
		}
		catch (JSQLParserException ex) {
			throw new IllegalArgumentException("The query you provided is not a valid SQL Query!", ex);
		}
	}

	@Override
	public String applySorting(Sort sort, String alias) {
		String queryString = this.query.getQueryString();
		Assert.hasText(queryString, "Query must not be null or empty!");

		if (this.parsedType != ParsedType.SELECT) {
			return queryString;
		}

		if (sort.isUnsorted()) {
			return queryString;
		}

		Select selectStatement = parseSelectStatement(queryString);
		PlainSelect selectBody = (PlainSelect) selectStatement.getSelectBody();

		final Set<String> joinAliases = this.getJoinAliases(selectBody);

		final Set<String> selectionAliases = this.getSelectionAliases(selectBody);

		List<OrderByElement> orderByElements = sort.stream() //
				.map(order -> this.getOrderClause(joinAliases, selectionAliases, alias, order)) //
				.collect(Collectors.toList());

		if (CollectionUtils.isEmpty(selectBody.getOrderByElements())) {
			selectBody.setOrderByElements(new ArrayList<>());
		}

		selectBody.getOrderByElements().addAll(orderByElements);

		return selectBody.toString();
	}

	private Set<String> getJoinAliases(String query) {

		if (this.parsedType != ParsedType.SELECT) {
			return new HashSet<>();
		}

		return this.getJoinAliases((PlainSelect) parseSelectStatement(query).getSelectBody());
	}

	private Set<String> getSelectionAliases(PlainSelect selectBody) {

		if (CollectionUtils.isEmpty(selectBody.getSelectItems())) {
			return new HashSet<>();
		}

		return selectBody.getSelectItems().stream() //
				.filter(SelectExpressionItem.class::isInstance) //
				.map(item -> ((SelectExpressionItem) item).getAlias()) //
				.filter(Objects::nonNull) //
				.map(Alias::getName) //
				.collect(Collectors.toSet());
	}

	Set<String> getSelectionAliases() {

		if (this.parsedType != ParsedType.SELECT) {
			return new HashSet<>();
		}

		Select selectStatement = parseSelectStatement(this.query.getQueryString());
		PlainSelect selectBody = (PlainSelect) selectStatement.getSelectBody();
		return this.getSelectionAliases(selectBody);
	}

	private Set<String> getJoinAliases(PlainSelect selectBody) {

		if (CollectionUtils.isEmpty(selectBody.getJoins())) {
			return new HashSet<>();
		}

		return selectBody.getJoins().stream() //
				.map(join -> join.getRightItem().getAlias()) //
				.filter(Objects::nonNull) //
				.map(Alias::getName) //
				.collect(Collectors.toSet());
	}

	private OrderByElement getOrderClause(final Set<String> joinAliases, final Set<String> selectionAliases,
			@Nullable final String alias, final Sort.Order order) {

		final OrderByElement orderByElement = new OrderByElement();
		orderByElement.setAsc(order.getDirection().isAscending());
		orderByElement.setAscDescPresent(true);

		final String property = order.getProperty();

		checkSortExpression(order);

		if (selectionAliases.contains(property)) {
			Expression orderExpression = order.isIgnoreCase() ? getJSqlLower(property) : new Column(property);

			orderByElement.setExpression(orderExpression);
			return orderByElement;
		}

		boolean qualifyReference = joinAliases //
				.parallelStream() //
				.map(joinAlias -> joinAlias.concat(".")) //
				.noneMatch(property::startsWith);

		boolean functionIndicator = property.contains("(");

		String reference = qualifyReference && !functionIndicator && StringUtils.hasText(alias)
				? String.format("%s.%s", alias, property) : property;
		Expression orderExpression = order.isIgnoreCase() ? getJSqlLower(reference) : new Column(reference);
		orderByElement.setExpression(orderExpression);
		return orderByElement;
	}

	@Override
	public String detectAlias() {
		return this.detectAlias(this.query.getQueryString());
	}

	@Nullable
	private String detectAlias(String query) {

		if (this.parsedType != ParsedType.SELECT) {
			return null;
		}

		Select selectStatement = parseSelectStatement(query);
		PlainSelect selectBody = (PlainSelect) selectStatement.getSelectBody();
		return detectAlias(selectBody);
	}

	@Nullable
	private static String detectAlias(PlainSelect selectBody) {

		Alias alias = selectBody.getFromItem().getAlias();
		return alias == null ? null : alias.getName();
	}

	@Override
	public String createCountQueryFor(String countProjection) {

		if (this.parsedType != ParsedType.SELECT) {
			return this.query.getQueryString();
		}

		Assert.hasText(this.query.getQueryString(), "OriginalQuery must not be null or empty!");

		Select selectStatement = parseSelectStatement(this.query.getQueryString());
		PlainSelect selectBody = (PlainSelect) selectStatement.getSelectBody();

		// remove order by
		selectBody.setOrderByElements(null);

		if (StringUtils.hasText(countProjection)) {
			Function jSqlCount = getJSqlCount(Collections.singletonList(countProjection), false);
			selectBody.setSelectItems(Collections.singletonList(new SelectExpressionItem(jSqlCount)));
			return selectBody.toString();
		}

		boolean distinct = selectBody.getDistinct() != null;
		selectBody.setDistinct(null); // reset possible distinct

		String tableAlias = detectAlias(selectBody);

		// is never null
		List<SelectItem> selectItems = selectBody.getSelectItems();

		if (this.onlyASingleColumnProjection(selectItems)) {
			SelectExpressionItem singleProjection = (SelectExpressionItem) selectItems.get(0);

			Column column = (Column) singleProjection.getExpression();
			String countProp = column.getFullyQualifiedName();

			Function jSqlCount = getJSqlCount(Collections.singletonList(countProp), distinct);
			selectBody.setSelectItems(Collections.singletonList(new SelectExpressionItem(jSqlCount)));
			return selectBody.toString();
		}

		String countProp = tableAlias == null ? "*" : tableAlias;

		Function jSqlCount = getJSqlCount(Collections.singletonList(countProp), distinct);
		selectBody.setSelectItems(Collections.singletonList(new SelectExpressionItem(jSqlCount)));

		return selectBody.toString();
	}

	@Override
	public String getProjection() {

		if (this.parsedType != ParsedType.SELECT) {
			return "";
		}

		Assert.hasText(this.query.getQueryString(), "Query must not be null or empty!");

		Select selectStatement = parseSelectStatement(this.query.getQueryString());
		PlainSelect selectBody = (PlainSelect) selectStatement.getSelectBody();

		return selectBody.getSelectItems() //
				.stream() //
				.map(Object::toString) //
				.collect(Collectors.joining(", ")).trim();
	}

	@Override
	public Set<String> getJoinAliases() {
		return this.getJoinAliases(this.query.getQueryString());
	}

	@Override
	public DeclaredQuery getQuery() {
		return this.query;
	}

	private static Select parseSelectStatement(String query) {

		try {
			return (Select) CCJSqlParserUtil.parse(query);
		}
		catch (JSQLParserException ex) {
			throw new IllegalArgumentException("The query you provided is not a valid SQL Query!", ex);
		}
	}

	private boolean onlyASingleColumnProjection(List<SelectItem> projection) {

		// this is unfortunately the only way to check without any hacky & hard string
		// regex magic
		return projection.size() == 1 && projection.get(0) instanceof SelectExpressionItem
				&& (((SelectExpressionItem) projection.get(0)).getExpression()) instanceof Column;
	}

	enum ParsedType {

		DELETE, UPDATE, SELECT

	}

}
