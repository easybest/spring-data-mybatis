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

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.pagination.RowSelection;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.query.DeclaredQuery;
import org.springframework.data.mybatis.repository.query.MybatisParameters;
import org.springframework.data.mybatis.repository.query.MybatisParameters.MybatisParameter;
import org.springframework.data.mybatis.repository.query.MybatisQueryMethod;
import org.springframework.data.mybatis.repository.query.QueryUtils;
import org.springframework.data.mybatis.repository.query.SimpleMybatisQuery;
import org.springframework.data.mybatis.repository.query.StringQuery;
import org.springframework.data.mybatis.repository.query.StringQuery.ParameterBinding;
import org.springframework.data.mybatis.repository.support.ResidentParameterName;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * .
 *
 * @author JARVIS SONG
 */
class SimpleMybatisQueryPrecompiler extends AbstractMybatisPrecompiler {

	private static Pattern patternIndex = Pattern.compile("[(\\d+)]");

	private static Pattern patternName = Pattern.compile("[(.+)]");

	private static Pattern patternString = Pattern.compile("'.+'");

	private static final Pattern SELECT_ALL_FROM = Pattern.compile("^\\s*select\\s+\\*\\s+from\\s+.*",
			Pattern.CASE_INSENSITIVE);

	private final SimpleMybatisQuery query;

	SimpleMybatisQueryPrecompiler(MybatisMappingContext mappingContext, SimpleMybatisQuery query) {
		super(mappingContext,
				mappingContext.getRequiredDomain(query.getQueryMethod().getEntityInformation().getJavaType()));
		this.query = query;
	}

	@Override
	protected Collection<String> prepareStatements() {

		String statement = this.doPrepareStatement();
		return StringUtils.isEmpty(statement) ? Collections.emptyList() : Collections.singletonList(statement);
	}

	private String doPrepareStatement() {
		if (this.checkStatement(this.query.getStatementName())) {
			return null;
		}

		switch (this.query.getSqlCommandType()) {

		case INSERT:
			return this.insertStatement();
		case UPDATE:
			return this.updateStatement();
		case DELETE:
			return this.deleteStatement();
		case SELECT:
			return this.selectStatement();
		default:
			throw new MappingException("Unsupported SQL Command Type: " + this.query.getSqlCommandType().name());
		}
	}

	private String selectStatement() {
		MybatisQueryMethod method = this.query.getQueryMethod();

		Map<String, Object> scopes = new HashMap<>();
		String sql = this.queryString(this.query.getQuery());

		if (QueryUtils.hasConstructorExpression(sql)) {
			String constructorExpression = QueryUtils.getConstructorExpression(sql);
			String className = constructorExpression.substring(3, constructorExpression.lastIndexOf('(')).trim();
			Class<?> clz;
			try {
				clz = ClassUtils.forName(className, this.getClass().getClassLoader());
			}
			catch (ClassNotFoundException ex) {
				throw new MappingException(
						"Could not find class " + className + " from constructor expression: " + sql);
			}
			String columns = constructorExpression.substring(constructorExpression.lastIndexOf('(') + 1,
					constructorExpression.length() - 1);
			String[] columnList = columns.split(",");
			sql = sql.replace(constructorExpression, Arrays.stream(columnList).map(
					c -> String.format("%s AS %s", c.trim(), this.mappingContext.getDialect().quoteCertainly(c.trim())))
					.collect(Collectors.joining(",")));
			sql = QueryUtils.quoteFieldAliases(sql, this.mappingContext.getDialect());

			Constructor<?> constructor = Arrays.stream(clz.getDeclaredConstructors())
					.filter(c -> c.getParameterCount() == columnList.length).findFirst().orElseThrow(
							() -> new MappingException("Could not find constructor for: " + constructorExpression));

			List<ConstructorParameter> constructorParameters = new LinkedList<>();

			for (int i = 0; i < constructor.getParameterCount(); i++) {
				Parameter parameter = constructor.getParameters()[i];
				constructorParameters.add(new ConstructorParameter(columnList[i], parameter.getType().getName()));
			}
			scopes.put("constructorParameters", constructorParameters);
			scopes.put("hasConstructor", true);
			scopes.put("isResultMap", true);
			scopes.put("result", ResidentStatementName.RESULT_MAP_PREFIX + this.query.getStatementName());
			scopes.put("resultMapType", className);
		}

		MybatisParameters parameters = method.getParameters();
		if (parameters.hasSortParameter()) {
			scopes.put("hasSort", true);
		}

		if (method.isPageQuery()) {
			scopes.put("isPageQuery", true);
			scopes.put("countStatementName", this.query.getCountStatementName());
			scopes.put("countQuery", this.queryString(this.query.getCountQuery()));
		}

		if (method.isPageQuery() || parameters.hasPageableParameter() || method.isSliceQuery()) {
			scopes.put("pageable", true);
			scopes.put("isUnpagedQuery", true);
			scopes.put("unpagedStatementName", ResidentStatementName.UNPAGED_PREFIX + this.query.getStatementName());
			scopes.put("unpagedQuery", sql);

			RowSelection rowSelection = new RowSelection(true);
			scopes.put("rowSelection", rowSelection);
		}

		if (null != method.getResultMap() || SELECT_ALL_FROM.matcher(sql.toLowerCase()).matches()) {
			scopes.putIfAbsent("isResultMap", true);
			if (null != method.getResultMap()) {
				scopes.putIfAbsent("result", method.getResultMap());
			}
			else {
				scopes.putIfAbsent("result", ResidentStatementName.RESULT_MAP);
			}
		}
		else {
			scopes.putIfAbsent("isResultMap", false);
			scopes.putIfAbsent("result", method.getActualResultType());
		}

		scopes.put("query", sql);
		return this.render("SimpleQuerySelect", scopes);
	}

	private String deleteStatement() {
		return this.render("SimpleQueryDelete", null);
	}

	private String updateStatement() {
		return this.render("SimpleQueryUpdate", null);
	}

	private String insertStatement() {
		return this.render("SimpleQueryInsert", null);
	}

	private String queryString(DeclaredQuery query) {
		String sql = query.getQueryString();
		sql = QueryUtils.quoteFieldAliases(sql, this.mappingContext.getDialect());

		List<ParameterBinding> parameterBindings = query.getParameterBindings();
		if (CollectionUtils.isEmpty(parameterBindings)) {
			return sql;
		}

		for (ParameterBinding parameterBinding : parameterBindings) {
			String replace;
			String bindName = null;
			String typeHandler = "";

			if (StringUtils.hasText(parameterBinding.getName())) {
				replace = ":" + parameterBinding.getRequiredName();
				bindName = parameterBinding.getName();
			}
			else {
				replace = "?" + parameterBinding.getPosition();
				if (parameterBinding.isExpression()) {
					Matcher matcher = patternIndex.matcher(parameterBinding.getExpression());
					if (matcher.find()) {
						String group = matcher.group(0);
						int position = Integer.valueOf(group) + 1;
						bindName = ResidentParameterName.POSITION_PREFIX + position;
					}
					else {
						matcher = patternName.matcher(parameterBinding.getExpression());
						if (matcher.find()) {
							bindName = matcher.group(0);
						}
						else if (patternString.matcher(parameterBinding.getExpression()).find()) {
							sql = sql.replace(replace, parameterBinding.getExpression());
						}
					}
				}
				else {
					MybatisParameter mp = this.query.getQueryMethod().getParameters()
							.getBindableParameter(parameterBinding.getRequiredPosition() - 1);
					bindName = mp.getName()
							.orElse(ResidentParameterName.POSITION_PREFIX + parameterBinding.getPosition());

				}
			}

			if (StringUtils.hasText(typeHandler)) {
				typeHandler = ",typeHandler=" + typeHandler;
			}
			else {
				typeHandler = "";
			}

			if (StringUtils.isEmpty(replace)) {
				return sql;
			}

			if (parameterBinding instanceof StringQuery.InParameterBinding) {
				sql = sql.replace(replace,

						String.format(
								"<foreach item=\"__item\" index=\"__index\" collection=\"%s\" open=\"(\" separator=\",\" close=\")\">#{__item%s}</foreach>",
								bindName, typeHandler));

				continue;
			}

			if (parameterBinding instanceof StringQuery.LikeParameterBinding) {

				StringQuery.LikeParameterBinding likeParameterBinding = (StringQuery.LikeParameterBinding) parameterBinding;

				String like = bindName;
				switch (likeParameterBinding.getType()) {
				case CONTAINING:
				case LIKE:
					like = "'%' + " + bindName + " + '%'";
					break;
				case STARTING_WITH:
					like = bindName + " + '%'";
					break;
				case ENDING_WITH:
					like = "'%' + " + bindName;
					break;
				}

				sql = sql.replace(replace, String.format("<bind name=\"__bind_%s\" value=\"%s\" />#{__bind_%s}",
						bindName, like, bindName));

				continue;

			}

			sql = sql.replace(replace, String.format("#{%s%s}", bindName, typeHandler));

		}
		return sql;
	}

	@Override
	protected String render(String name, Map<String, Object> scopes) {
		if (null == scopes) {
			scopes = new HashMap<>();
		}
		scopes.putIfAbsent(SCOPE_STATEMENT_NAME, this.query.getStatementName());
		scopes.putIfAbsent("query", this.queryString(this.query.getQuery()));
		return super.render(name, scopes);
	}

	@Override
	protected String getResource(String dir, String namespace) {
		return "simple/" + this.query.getStatementId().replace('.', '/');
	}

	@Data
	@AllArgsConstructor
	public static class ConstructorParameter {

		private String column;

		private String javaType;

	}

}
