package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * Created by songjiawei on 2016/11/10.
 */
public abstract class AbstractStringBasedMybatisQuery extends AbstractMybatisQuery {

    private final StringQuery               query;
    private final EvaluationContextProvider evaluationContextProvider;
    private final SpelExpressionParser      parser;


    protected AbstractStringBasedMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method, String queryString,
                                              EvaluationContextProvider evaluationContextProvider,
                                              SpelExpressionParser parser) {
        super(sqlSessionTemplate, method);

        Assert.hasText(queryString, "Query string must not be null or empty!");
        Assert.notNull(evaluationContextProvider, "ExpressionEvaluationContextProvider must not be null!");
        Assert.notNull(parser, "Parser must not be null or empty!");

        this.evaluationContextProvider = evaluationContextProvider;
        this.query = new ExpressionBasedStringQuery(queryString, method.getEntityInformation(), parser);
        this.parser = parser;
    }


}
