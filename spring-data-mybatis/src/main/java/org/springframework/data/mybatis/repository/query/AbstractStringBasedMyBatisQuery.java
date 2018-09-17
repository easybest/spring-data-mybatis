package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * @author Jarvis Song
 */
abstract class AbstractStringBasedMyBatisQuery extends AbstractMyBatisQuery {

	private final DeclaredQuery query;
	private final DeclaredQuery countQuery;
	private final SpelExpressionParser parser;

	public AbstractStringBasedMyBatisQuery(SqlSessionTemplate template, MyBatisQueryMethod method, String queryString,
			SpelExpressionParser parser) {
		super(template, method);
		Assert.hasText(queryString, "Query string must not be null or empty!");
		Assert.notNull(parser, "Parser must not be null or empty!");

		this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation(), parser);
		this.countQuery = query.deriveCountQuery(method.getCountQuery(), method.getCountQueryProjection());

		this.parser = parser;
	}


	public DeclaredQuery getQuery() {
		return query;
	}

	public DeclaredQuery getCountQuery() {
		return countQuery;
	}
}
