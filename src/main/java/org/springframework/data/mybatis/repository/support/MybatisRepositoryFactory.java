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

package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.repository.localism.Localism;
import org.springframework.data.mybatis.repository.query.MybatisQueryLookupStrategy;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.util.Assert;

import java.io.Serializable;

import static org.springframework.data.querydsl.QueryDslUtils.QUERY_DSL_PRESENT;

/**
 * mybatis implementation of repository factory.
 *
 * @author Jarvis Song
 */
public class MybatisRepositoryFactory extends RepositoryFactorySupport {

    private final SqlSessionTemplate sessionTemplate;
    private final Localism           localism;

    private static final String SCRIPT_LANG = "beetl";

    public MybatisRepositoryFactory(final SqlSessionTemplate sessionTemplate, Localism localism) {
        Assert.notNull(sessionTemplate);
        Assert.notNull(localism);
        this.localism = localism;
        this.sessionTemplate = sessionTemplate;
    }

    @Override
    public <T, ID extends Serializable> MybatisEntityInformationSupport<T, ID> getEntityInformation(Class<T> domainClass) {
        return (MybatisEntityInformationSupport<T, ID>) MybatisEntityInformationSupport.getEntityInformation(domainClass);
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        return getTargetRepository(sessionTemplate, localism, information);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        if (isQueryDslExecutor(metadata.getRepositoryInterface())) {
            return QueryDslMybatisRepository.class;
        } else {
            return SimpleMybatisRepository.class;
        }
    }

    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
        return MybatisQueryLookupStrategy.create(sessionTemplate, localism, key, evaluationContextProvider);
    }

    protected <T, ID extends Serializable> SimpleMybatisRepository<?, ?> getTargetRepository(
            SqlSessionTemplate sessionTemplate, Localism localism, RepositoryInformation information) {

        MybatisEntityInformationSupport<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());

        generateMapper(sessionTemplate.getConfiguration(), entityInformation, localism);

        return getTargetRepositoryViaReflection(information, entityInformation, sessionTemplate);
    }

    private void generateMapper(Configuration configuration, MybatisEntityInformationSupport<?, ?> entityInformation, Localism localism) {
        new MybatisSimpleRepositoryMapperGenerator(configuration, entityInformation.getModel(), localism, SCRIPT_LANG).generate();
    }


    private boolean isQueryDslExecutor(Class<?> repositoryInterface) {
        return QUERY_DSL_PRESENT && QueryDslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
    }


}
