package org.springframework.data.mybatis.repository.query;

import org.springframework.expression.spel.standard.SpelExpressionParser;

enum MyBatisQueryFactory {

	INSTANCE;

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

}
