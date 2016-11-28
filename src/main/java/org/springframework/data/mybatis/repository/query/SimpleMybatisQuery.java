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
import org.springframework.data.mybatis.repository.annotation.Query;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * simple implementation of {@link org.springframework.data.repository.query.RepositoryQuery}.
 *
 * @author Jarvis Song
 */
public class SimpleMybatisQuery extends AbstractMybatisQuery {


    public SimpleMybatisQuery(SqlSessionTemplate sqlSessionTemplate, MybatisQueryMethod method, Query query, EvaluationContextProvider evaluationContextProvider, SpelExpressionParser parser) {
        super(sqlSessionTemplate, method);
    }


}
