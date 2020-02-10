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

import java.util.stream.Stream;

import org.apache.ibatis.mapping.SqlCommandType;
import org.mybatis.spring.SqlSessionTemplate;

import org.springframework.data.mybatis.repository.Modifying;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.util.Lazy;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link String} based MyBatis queries.
 *
 * @author JARVIS SONG
 */
abstract class AbstractStringBasedMybatisQuery extends AbstractMybatisQuery {

	private final DeclaredQuery query;

	private final DeclaredQuery countQuery;

	private final QueryMethodEvaluationContextProvider evaluationContextProvider;

	private final SpelExpressionParser parser;

	private final Lazy<SqlCommandType> sqlCommandType;

	AbstractStringBasedMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method,
			String queryString, String countQueryString, QueryMethodEvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser parser) {
		super(sqlSessionTemplate, method);

		Assert.hasText(queryString, "Query string must not be null or empty!");
		Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null!");
		Assert.notNull(parser, "Parser must not be null!");

		this.evaluationContextProvider = evaluationContextProvider;
		this.parser = parser;

		this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation(), parser);
		if (StringUtils.hasText(countQueryString)) {
			this.countQuery = new ExpressionBasedStringQuery(countQueryString, method.getEntityInformation(), parser);
		}
		else {
			this.countQuery = this.query.deriveCountQuery(null, null);
		}

		this.sqlCommandType = Lazy.of(() -> {
			if (method.isModifyingQuery() && method.getModifyingType() != Modifying.TYPE.SELECT) {
				return SqlCommandType.valueOf(method.getModifyingType().name());
			}

			return Stream.of(Modifying.TYPE.values())
					.filter(type -> queryString.toUpperCase().startsWith(type.name() + " "))
					.map(type -> SqlCommandType.valueOf(type.name())).findFirst().orElse(SqlCommandType.UNKNOWN);

		});
	}

	@Override
	public SqlCommandType getSqlCommandType() {
		return this.sqlCommandType.get();
	}

	public DeclaredQuery getQuery() {
		return this.query;
	}

	public DeclaredQuery getCountQuery() {
		return this.countQuery;
	}

	public QueryMethodEvaluationContextProvider getEvaluationContextProvider() {
		return this.evaluationContextProvider;
	}

	public SpelExpressionParser getParser() {
		return this.parser;
	}

}
