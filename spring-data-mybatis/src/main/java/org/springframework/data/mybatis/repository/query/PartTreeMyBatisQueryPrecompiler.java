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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.mapping.MybatisPersistentProperty;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.StringUtils;

/**
 * Part tree query precompiler.
 *
 * @author JARVIS SONG
 */
class PartTreeMyBatisQueryPrecompiler extends AbstractMybatisPrecompiler {

	private final PartTreeMybatisQuery query;

	private int count = 0;

	PartTreeMyBatisQueryPrecompiler(MybatisMappingContext mappingContext, Configuration configuration,
			PartTreeMybatisQuery query) {
		super(mappingContext, configuration, query.getQueryMethod().getNamespace(),
				query.getQueryMethod().getEntityInformation().getJavaType());

		this.query = query;
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
		if (method.isCollectionQuery()) {
			return this.addCollectionStatement();
		}

		if (method.isQueryForEntity()) {
			return this.buildSelectStatement(this.query.getQueryMethod().getStatementName(), false);
		}

		return null;
	}

	private String addPageStatement(boolean includeCount) {
		String sql = this.buildSelectStatement(this.query.getQueryMethod().getStatementName(), true);
		sql += this.buildSelectStatement(
				ResidentStatementName.UNPAGED_PREFIX + this.query.getQueryMethod().getStatementName(), false);
		if (includeCount) {
			String count = this.buildCountStatement(
					ResidentStatementName.COUNT_PREFIX + this.query.getQueryMethod().getStatementName());
			return sql + count;
		}
		return sql;
	}

	private String addExistsStatement() {
		return this.buildCountStatement(this.query.getQueryMethod().getStatementName());
	}

	private String addDeleteStatement(PartTree tree, MybatisQueryMethod method) {

		String where = this.buildTreeOrConditionSegment(tree, method);
		String sql = String.format("<delete id=\"%s\">delete from %s %s</delete>", //
				method.getStatementName(), this.getTableName(), StringUtils.hasText(where) ? (" where " + where) : "");
		if (method.isCollectionQuery()) {
			String query = this.buildSelectStatement(ResidentStatementName.QUERY_PREFIX + method.getStatementName(),
					false);
			return query + sql;
		}
		return sql;
	}

	private String addCountStatement() {
		return this.buildCountStatement(this.query.getQueryMethod().getStatementName());
	}

	private String addCollectionStatement() {

		return this.buildSelectStatement(this.query.getQueryMethod().getStatementName(), false);
	}

	private String buildCountStatement(String statementName) {
		this.count = 0;
		PartTree tree = this.query.getTree();
		MybatisQueryMethod method = this.query.getQueryMethod();

		String where = this.buildTreeOrConditionSegment(tree, method);
		String sql = String.format("select %s from %s %s", tree.isLimiting() ? "1" : "count(*)", this.getTableName(),
				StringUtils.hasText(where) ? (" where " + where) : "");

		if (tree.isLimiting()) {
			RowSelection rowSelection = new RowSelection();
			rowSelection.setMaxRows(tree.getMaxResults());

			sql = String.format("select count(*) from (%s) __a",
					this.dialect.getLimitHandler().processSql(sql, rowSelection));
		}

		return String.format("<select id=\"%s\" resultType=\"long\">%s</select>", statementName, sql);
	}

	private String buildSelectStatement(String statementName, boolean pageable) {
		this.count = 0;
		PartTree tree = this.query.getTree();
		MybatisQueryMethod method = this.query.getQueryMethod();

		String columns = "*";
		if (StringUtils.hasText(method.getSelectColumns())) {
			String[] selectColumns = method.getSelectColumns().split(",");
			columns = Stream.of(selectColumns).map(String::trim).filter(StringUtils::hasText).map(
					sc -> this.persistentEntity.getRequiredPersistentProperty(sc).getColumn().getName().render(dialect))
					.collect(Collectors.joining(","));
		}

		String where = this.buildTreeOrConditionSegment(tree, method);
		String sort = "";
		if (null != tree.getSort() || method.getParameters().hasSortParameter()) {
			sort = this.buildStandardOrderBySegment();
		}
		String sql = String.format("select %s %s from %s %s %s", tree.isDistinct() ? "distinct" : "", columns,
				this.getTableName(), StringUtils.hasText(where) ? (" where " + where) : "", sort);

		RowSelection rowSelection = null;
		if (tree.isLimiting()) {
			if (!pageable) {
				rowSelection = new RowSelection();
				rowSelection.setMaxRows(tree.getMaxResults());
			}

			pageable = true;
		}

		if (pageable) {
			sql = this.dialect.getLimitHandler().processSql(sql, rowSelection);
		}
		String result = String.format("resultMap=\"%s\"", ResidentStatementName.RESULT_MAP);
		if (StringUtils.hasText(method.getResultMap())) {
			result = String.format("resultMap=\"%s\"", method.getResultMap());
		}
		else if (null != method.getResultType()) {
			if (method.getResultType() == Void.class) {
				result = String.format("resultType=\"%\"", method.getActualResultType());
			}
			else {
				result = String.format("resultType=\"%\"", method.getResultType().getName());
			}
		}

		return String.format("<select id=\"%s\" %s>%s</select>", statementName, result, sql);
	}

	private String buildTreeOrConditionSegment(PartTree tree, MybatisQueryMethod method) {
		return tree.stream().map(node -> String.format("(%s)", this.buildTreeAndConditionSegment(node)))
				.collect(Collectors.joining(" or "));
	}

	private String buildTreeAndConditionSegment(PartTree.OrPart parts) {
		return parts.stream().map(part -> this.buildTreePredicateSegment(part)).collect(Collectors.joining(" and "));
	}

	private String buildTreePredicateSegment(Part part) {

		StringBuilder builder = new StringBuilder();

		PropertyPath property = part.getProperty();
		Part.Type type = part.getType();

		MybatisPersistentProperty persistentProperty = this.persistentEntity
				.getRequiredPersistentProperty(property.getSegment());

		builder.append(
				this.buildQueryByConditionLeftSegment(persistentProperty.getColumn().getName().render(this.dialect),
						part.shouldIgnoreCase(), persistentProperty));
		builder.append(this.buildQueryByConditionOperator(type));

		String[] properties = new String[type.getNumberOfArguments()];
		if (type.getNumberOfArguments() > 0) {
			MybatisQueryMethod method = this.query.getQueryMethod();
			MybatisParameters parameters = method.getParameters();

			for (int i = 0; i < type.getNumberOfArguments(); i++) {
				MybatisParameters.MybatisParameter parameter = parameters.getBindableParameter(this.count++);
				properties[i] = parameter.isNamedParameter() ? parameter.getName().get()
						: "__p" + (parameter.getIndex() + 1);
			}
		}

		builder.append(this.buildQueryByConditionRightSegment(type, part.shouldIgnoreCase(), properties));
		return builder.toString();
	}

	@Override
	protected String getResourceSuffix() {
		return "_tree_" + this.query.getQueryMethod().getStatementName() + super.getResourceSuffix();
	}

}
