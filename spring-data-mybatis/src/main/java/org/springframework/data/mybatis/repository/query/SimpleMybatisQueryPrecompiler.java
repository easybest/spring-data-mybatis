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

import org.apache.ibatis.session.Configuration;

import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;
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
					ResidentStatementName.UNPAGED_PREFIX + this.query.getStatementName(), this.resultMapOrType(sql),
					sql + sort);
			sql = this.dialect.getLimitHandler().processSql(sql + sort, null);
		}

		String select = String.format("<select id=\"%s\" %s>%s</select>", this.query.getStatementName(),
				this.resultMapOrType(sql), sql);

		return select + unpaged + count;

	}

	@Override
	protected String getResourceSuffix() {
		return "_simple_" + this.query.getQueryMethod().getStatementName() + super.getResourceSuffix();
	}

}
