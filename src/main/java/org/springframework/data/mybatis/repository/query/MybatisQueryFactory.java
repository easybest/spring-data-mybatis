package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mybatis.repository.annotation.Query;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Created by songjiawei on 2016/11/10.
 */
public enum MybatisQueryFactory {

    INSTANCE;
    private transient static final Logger               LOG    = LoggerFactory.getLogger(MybatisQueryFactory.class);
    private static final           SpelExpressionParser PARSER = new SpelExpressionParser();

    AbstractMybatisQuery fromQueryAnnotation(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method, EvaluationContextProvider evaluationContextProvider) {
        LOG.debug("Looking up query for method {}", method.getName());

        Query query = method.getQueryAnnotation();
        if (null == query) {
            return null;
        }


        return fromMethodWithQueryString(sqlSessionTemplate, method, method.getAnnotatedQuery(), evaluationContextProvider);
    }

    AbstractMybatisQuery fromMethodWithQueryString(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method, String queryString,
                                                   EvaluationContextProvider evaluationContextProvider) {
        if (null == queryString) {
            return null;
        }

        return new SimpleMybatisQuery(sqlSessionTemplate, method, queryString, evaluationContextProvider, PARSER);
    }
}
