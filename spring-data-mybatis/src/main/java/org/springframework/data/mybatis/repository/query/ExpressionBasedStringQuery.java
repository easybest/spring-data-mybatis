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

import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * .
 *
 * @author JARVIS SONG
 */
class ExpressionBasedStringQuery extends StringQuery {

	private static final String EXPRESSION_PARAMETER = "?#{";

	private static final String QUOTED_EXPRESSION_PARAMETER = "?__HASH__{";

	private static final Pattern EXPRESSION_PARAMETER_QUOTING = Pattern.compile(Pattern.quote(EXPRESSION_PARAMETER));

	private static final Pattern EXPRESSION_PARAMETER_UNQUOTING = Pattern
			.compile(Pattern.quote(QUOTED_EXPRESSION_PARAMETER));

	private static final String ENTITY_NAME = "entityName";

	private static final String TABLE_NAME = "tableName";

	private static final String ENTITY_NAME_VARIABLE = "#" + ENTITY_NAME;

	private static final String ENTITY_NAME_VARIABLE_EXPRESSION = "#{" + ENTITY_NAME_VARIABLE + "}";

	ExpressionBasedStringQuery(String query, MybatisEntityMetadata<?> metadata, SpelExpressionParser parser) {
		super(renderQueryIfExpressionOrReturnQuery(query, metadata, parser));
	}

	static ExpressionBasedStringQuery from(DeclaredQuery query, MybatisEntityMetadata<?> metadata,
			SpelExpressionParser parser) {
		return new ExpressionBasedStringQuery(query.getQueryString(), metadata, parser);
	}

	private static String renderQueryIfExpressionOrReturnQuery(String query, MybatisEntityMetadata<?> metadata,
			SpelExpressionParser parser) {

		Assert.notNull(query, "query must not be null!");
		Assert.notNull(metadata, "metadata must not be null!");
		Assert.notNull(parser, "parser must not be null!");

		if (!containsExpression(query)) {
			return query;
		}

		StandardEvaluationContext evalContext = new StandardEvaluationContext();
		// evalContext.setVariable(ENTITY_NAME, metadata.getEntityName());
		evalContext.setVariable(ENTITY_NAME, metadata.getTableName());
		evalContext.setVariable(TABLE_NAME, metadata.getTableName());

		query = potentiallyQuoteExpressionsParameter(query);

		Expression expr = parser.parseExpression(query, ParserContext.TEMPLATE_EXPRESSION);

		String result = expr.getValue(evalContext, String.class);

		if (result == null) {
			return query;
		}

		return potentiallyUnquoteParameterExpressions(result);
	}

	private static String potentiallyUnquoteParameterExpressions(String result) {
		return EXPRESSION_PARAMETER_UNQUOTING.matcher(result).replaceAll(EXPRESSION_PARAMETER);
	}

	private static String potentiallyQuoteExpressionsParameter(String query) {
		return EXPRESSION_PARAMETER_QUOTING.matcher(query).replaceAll(QUOTED_EXPRESSION_PARAMETER);
	}

	private static boolean containsExpression(String query) {
		return query.contains(ENTITY_NAME_VARIABLE_EXPRESSION);
	}

}
