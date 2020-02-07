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

import java.util.regex.Pattern;

import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;

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

		return String.format("<select id=\"%s\" %s=\"%s\">%s</select>", method.getStatementName(),
				((null != method.getResultMap() || SELECT_ALL_FROM.matcher(sql).matches()) ? "resultMap"
						: "resultType"),
				((null != method.getResultMap()) ? //
						method.getResultMap() : //
						(SELECT_ALL_FROM.matcher(sql).matches() ? //
								ResidentStatementName.RESULT_MAP : //
								method.getReturnedObjectType().getName())),
				sql);
	}

	@Override
	protected String getResourceSuffix() {
		return "_" + this.query.getQueryMethod().getStatementName() + super.getResourceSuffix();
	}

}
