package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Created by songjiawei on 2016/11/10.
 */
public class SimpleMybatisQuery extends AbstractStringBasedMybatisQuery {


    protected SimpleMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method, String queryString, EvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {
        super(sqlSessionTemplate, method, queryString, evaluationContextProvider, parser);
    }
}
