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

import org.apache.ibatis.session.Configuration;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mybatis.dialect.Dialect;
import org.springframework.data.mybatis.mapping.MybatisMappingContext;
import org.springframework.data.mybatis.repository.support.ResidentStatementName;

/**
 * .
 *
 * @author JARVIS SONG
 */
abstract class MybatisQueryMethodPrecompiler extends AbstractMybatisPrecompiler {

	private static final Pattern SELECT_ALL_FROM = Pattern.compile("^\\s*select\\s+\\*\\s+from\\s+.*",
			Pattern.CASE_INSENSITIVE);

	protected final AbstractMybatisQuery query;

	MybatisQueryMethodPrecompiler(MybatisMappingContext mappingContext, Configuration configuration, Dialect dialect,
			AbstractMybatisQuery query) {
		super(mappingContext, configuration, dialect, query.getQueryMethod().getEntityInformation().getJavaType());

		this.query = query;
	}

	@Override
	protected String doPrecompile() {

		if (this.configuration.hasStatement(this.query.getStatementId(), false)) {
			return "";
		}

		switch (this.query.getSqlCommandType()) {

		case INSERT:
			return this.insert();
		case UPDATE:
			return this.update();
		case DELETE:
			return this.delete();
		case SELECT:
			return this.select();
		default:
			throw new MappingException("Unsupported SQL Command Type: " + this.query.getSqlCommandType().name());

		}

	}

	protected String insert() {
		return String.format("<insert id=\"%s\">%s</insert>", this.query.getStatementName(), this.mainQueryString());
	}

	protected String update() {
		return String.format("<update id=\"%s\">%s</update>", this.query.getStatementName(), this.mainQueryString());
	}

	protected String delete() {
		return String.format("<delete id=\"%s\">%s</delete>", this.query.getStatementName(), this.mainQueryString());
	}

	protected String select() {
		String sql = this.mainQueryString();
		return String.format("<select id=\"%s\" %s>%s</select>", this.query.getStatementName(),
				this.resultMapOrType(sql), sql);
	}

	protected String resultMapOrType(String sql) {
		MybatisQueryMethod method = this.query.getQueryMethod();

		return String.format("%s=\"%s\"",
				(null != method.getResultMap() || SELECT_ALL_FROM.matcher(sql.toLowerCase()).matches()) ? "resultMap"
						: "resultType",
				((null != method.getResultMap()) ? method.getResultMap() : //
						(SELECT_ALL_FROM.matcher(sql).matches() ? //
								ResidentStatementName.RESULT_MAP : //
								method.getActualResultType())));
	}

	protected abstract String mainQueryString();

}
