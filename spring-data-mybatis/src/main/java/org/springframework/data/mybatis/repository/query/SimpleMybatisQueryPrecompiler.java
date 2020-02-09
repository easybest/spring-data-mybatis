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

import java.util.List;
import java.util.regex.Pattern;

import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Precompiler for {@link SimpleMybatisQuery}.
 *
 * @author JARVIS SONG
 */
class SimpleMybatisQueryPrecompiler extends AbstractMybatisPrecompiler {

	private static final Pattern SELECT_ALL_FROM = Pattern.compile("^\\s*select\\s+\\*\\s+from\\s+.*",
			Pattern.CASE_INSENSITIVE);

	private final SimpleMybatisQuery query;

	SimpleMybatisQueryPrecompiler(MybatisMappingContext mappingContext, Configuration configuration,
			SimpleMybatisQuery query) {

		super(mappingContext, configuration, query.getQueryMethod().getNamespace(),
				query.getQueryMethod().getEntityInformation().getJavaType());

		this.query = query;
	}

	@Override
	protected String doPrecompile() {

		MybatisQueryMethod method = this.query.getQueryMethod();
		if (this.configuration.hasStatement(method.getStatementId(), false)) {
			return "";
		}
		String sql = this.query.getQuery().getQueryString();
		List<StringQuery.ParameterBinding> parameterBindings = this.query.getQuery().getParameterBindings();
		if (!CollectionUtils.isEmpty(parameterBindings)) {
			sql = this.parseMybatisSQL(sql, parameterBindings, method);
		}

		if (method.isModifyingQuery()) {
			SqlCommandType type = method.getModifyingType();
			if (null == type) {
				type = this.extractSqlCommandType(sql);
			}
			if (null == type || type == SqlCommandType.UNKNOWN) {
				throw new MappingException("@Modifying SQL type is UNKNOWN for " + method.getName());
			}
			switch (type) {

			case INSERT:
				return String.format("<insert id=\"%s\">%s</insert>", method.getStatementName(), sql);
			case UPDATE:
				return String.format("<update id=\"%s\">%s</update>", method.getStatementName(), sql);
			case DELETE:
				return String.format("<delete id=\"%s\">%s</delete>", method.getStatementName(), sql);
			}
			return "";
		}

		String unpaged = "";
		if (method.isPageQuery()) {
			unpaged = String.format("<select id=\"%s\" %s=\"%s\">%s</select>",
					ResidentStatementName.UNPAGED_PREFIX + method.getStatementName(),
					((null != method.getResultMap() || SELECT_ALL_FROM.matcher(sql).matches()) ? "resultMap"
							: "resultType"),
					((null != method.getResultMap()) ? //
							method.getResultMap() : //
							(SELECT_ALL_FROM.matcher(sql).matches() ? //
									ResidentStatementName.RESULT_MAP : //
									method.getReturnedObjectType().getName())),
					sql);
		}
		if (method.isPageQuery() || method.isSliceQuery()) {
			sql = this.dialect.getLimitHandler().processSql(sql, null);
		}

		String select = String.format("<select id=\"%s\" %s=\"%s\">%s</select>", method.getStatementName(),
				((null != method.getResultMap() || SELECT_ALL_FROM.matcher(sql).matches()) ? "resultMap"
						: "resultType"),
				((null != method.getResultMap()) ? //
						method.getResultMap() : //
						(SELECT_ALL_FROM.matcher(sql).matches() ? //
								ResidentStatementName.RESULT_MAP : //
								method.getActualResultType())),
				sql);
		if (method.isPageQuery()) {
			DeclaredQuery countQuery = this.query.getQuery().deriveCountQuery(null, null);
			String count = String.format("<select id=\"%s\" resultType=\"long\">%s</select>",
					ResidentStatementName.COUNT_PREFIX + method.getStatementName(), countQuery.getQueryString());
			return select + count + unpaged;
		}
		return select + unpaged;
	}

	private String parseMybatisSQL(String sql, List<StringQuery.ParameterBinding> parameterBindings,
			MybatisQueryMethod method) {

		for (StringQuery.ParameterBinding parameterBinding : parameterBindings) {
			String replace = null;
			String bindName = null;
			String typeHandler = "";

			if (StringUtils.hasText(parameterBinding.getName())) {
				replace = ":" + parameterBinding.getName();
				bindName = parameterBinding.getName();
			}
			else {
				replace = "?" + parameterBinding.getPosition();

				if (parameterBinding.isExpression()) {
					bindName = "__p" + parameterBinding.getPosition();
				}
				else {
					MybatisParameters.MybatisParameter mp = method.getParameters()
							.getBindableParameter(parameterBinding.getRequiredPosition() - 1);
					// bindName = mp.getName().orElse("__p" + mp.getIndex());
					bindName = mp.getName().orElse("__p" + parameterBinding.getPosition());
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
	protected String getResourceSuffix() {
		return "_" + this.query.getQueryMethod().getStatementName() + super.getResourceSuffix();
	}

}
