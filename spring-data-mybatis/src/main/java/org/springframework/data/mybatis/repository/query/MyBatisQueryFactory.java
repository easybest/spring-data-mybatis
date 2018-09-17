package org.springframework.data.mybatis.repository.query;

import lombok.extern.slf4j.Slf4j;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

@Slf4j
enum MyBatisQueryFactory {

	INSTANCE;

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	@Nullable
	AbstractMyBatisQuery fromMethodWithQueryString(SqlSessionTemplate sqlSessionTemplate, MyBatisQueryMethod method,
			@Nullable String queryString, EvaluationContextProvider evaluationContextProvider) {

		if (StringUtils.isEmpty(queryString)) {
			return null;
		}

		return new SimpleMyBatisQuery(sqlSessionTemplate, method);
	}

}
