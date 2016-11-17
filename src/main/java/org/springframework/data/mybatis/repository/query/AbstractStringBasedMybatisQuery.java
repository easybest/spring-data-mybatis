/*
 *
 *   Copyright 2016 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.springframework.data.mybatis.repository.query;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * string based mybatis query .
 *
 * @author Jarvis Song
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
