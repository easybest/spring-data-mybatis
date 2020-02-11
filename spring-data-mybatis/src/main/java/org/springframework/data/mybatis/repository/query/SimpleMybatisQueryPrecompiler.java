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

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Precompiler for {@link SimpleMybatisQuery}.
 *
 * @author JARVIS SONG
 */
class SimpleMybatisQueryPrecompiler extends MybatisQueryMethodPrecompiler {

	private final SimpleMybatisQuery query;

	SimpleMybatisQueryPrecompiler(MybatisMappingContext mappingContext, Configuration configuration,
			SimpleMybatisQuery query) {

		super(mappingContext, configuration, query);

		this.query = query;
	}

	@Override
	protected String mainQueryString() {
		return this.queryString(this.query.getQuery());
	}

	private String queryString(DeclaredQuery query) {

		String sql = query.getQueryString();

		sql = QueryUtils.quoteFieldAliases(sql, this.dialect);

		List<StringQuery.ParameterBinding> parameterBindings = query.getParameterBindings();
		if (CollectionUtils.isEmpty(parameterBindings)) {
			return sql;
		}
		for (StringQuery.ParameterBinding parameterBinding : parameterBindings) {
			String replace = null;
			String bindName = null;
			String typeHandler = "";

			if (StringUtils.hasText(parameterBinding.getName())) {
				replace = ":" + parameterBinding.getRequiredName();
				bindName = parameterBinding.getName();
			}
			else {
				replace = "?" + parameterBinding.getPosition();
				if (parameterBinding.isExpression()) {
					bindName = ResidentStatementName.PARAMETER_POSITION_PREFIX + parameterBinding.getPosition();
				}
				else {
					MybatisParameters.MybatisParameter mp = this.query.getQueryMethod().getParameters()
							.getBindableParameter(parameterBinding.getRequiredPosition() - 1);
					bindName = mp.getName()
							.orElse(ResidentStatementName.PARAMETER_POSITION_PREFIX + parameterBinding.getPosition());
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

				sql = sql.replace(replace, String.format("<bind name=\"__bind_%s\" value=\"%s\" /> #{__bind_%s}",
						bindName, like, bindName));

				continue;

			}

			sql = sql.replace(replace, String.format("#{%s%s}", bindName, typeHandler));
		}

		return sql;
	}

	@Override
	protected String select() {

		MybatisQueryMethod method = this.query.getQueryMethod();
		String sql = this.mainQueryString();
		String unpaged = "";
		String sort = "";
		String count = "";
		String resultMap = null;
		String result = "";

		if (QueryUtils.hasConstructorExpression(sql)) {
			String constructorExpression = QueryUtils.getConstructorExpression(sql);

			String className = constructorExpression.substring(3, constructorExpression.lastIndexOf("(")).trim();
			Class<?> clz;
			try {
				clz = ClassUtils.forName(className, this.getClass().getClassLoader());

			}
			catch (ClassNotFoundException ex) {
				throw new MappingException(
						"Could not find class: " + className + " from constructor expression: " + sql);
			}
			String columns = constructorExpression.substring(constructorExpression.lastIndexOf("(") + 1,
					constructorExpression.length() - 1);
			String[] columnList = columns.split(",");

			sql = sql.replace(constructorExpression,
					Stream.of(columnList)
							.map(c -> String.format("%s as %s", c.trim(), dialect.quoteCertainly(c.trim())))
							.collect(Collectors.joining(",")));
			sql = QueryUtils.quoteFieldAliases(sql, this.dialect);
			StringBuilder results = new StringBuilder();
			results.append("<constructor>");
			Constructor<?> constructor = Stream.of(clz.getDeclaredConstructors())
					.filter(c -> c.getParameterCount() == columnList.length).findFirst().orElseThrow(
							() -> new MappingException("Could not find constructor for: " + constructorExpression));

			for (int i = 0; i < constructor.getParameterCount(); i++) {
				Parameter parameter = constructor.getParameters()[i];
				results.append(String.format("<arg column=\"%s\" javaType=\"%s\"/>", columnList[i].trim(),
						parameter.getType().getName()));
			}

			results.append("</constructor>");
			resultMap = ResidentStatementName.RESULT_MAP_PREFIX + this.query.getStatementName();

			result = String.format("<resultMap id=\"%s\" type=\"%s\">%s</resultMap>", resultMap, className,
					results.toString());

			resultMap = String.format(" resultMap=\"%s\"", resultMap);
		}

		MybatisParameters parameters = method.getParameters();
		if (parameters.hasSortParameter()) {
			sort = this.buildStandardOrderBySegment();
		}

		if (method.isPageQuery()) {
			count = String.format("<select id=\"%s\" resultType=\"long\">%s</select>",
					this.query.getCountStatementName(), this.queryString(this.query.getCountQuery()));
		}

		if (method.isPageQuery() || parameters.hasPageableParameter() || method.isSliceQuery()) {
			unpaged = String.format("<select id=\"%s\" %s>%s</select>",
					ResidentStatementName.UNPAGED_PREFIX + this.query.getStatementName(),
					(null != resultMap) ? resultMap : this.resultMapOrType(sql), sql + sort);
			sql = this.dialect.getLimitHandler().processSql(sql + sort, null);
		}

		String select = String.format("<select id=\"%s\" %s>%s</select>", this.query.getStatementName(),
				(null != resultMap) ? resultMap : this.resultMapOrType(sql), sql);

		return result + select + unpaged + count;

	}

	@Override
	protected String getResourceSuffix() {
		return "_simple_" + this.query.getQueryMethod().getStatementName() + super.getResourceSuffix();
	}

}
