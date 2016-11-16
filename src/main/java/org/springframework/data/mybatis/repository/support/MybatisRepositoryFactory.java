package org.springframework.data.mybatis.repository.support;

import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
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
 * Created by songjiawei on 2016/11/9.
 */
public class MybatisRepositoryFactory extends RepositoryFactorySupport {

    private final SqlSessionTemplate sessionTemplate;

    public MybatisRepositoryFactory(final SqlSessionTemplate sessionTemplate) {
        Assert.notNull(sessionTemplate);
        this.sessionTemplate = sessionTemplate;
    }

    @Override
    public <T, ID extends Serializable> MybatisEntityInformationSupport<T, ID> getEntityInformation(Class<T> domainClass) {
        return (MybatisEntityInformationSupport<T, ID>) MybatisEntityInformationSupport.getEntityInformation(domainClass);
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        return getTargetRepository(sessionTemplate, information);
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
        return MybatisQueryLookupStrategy.create(sessionTemplate, key, evaluationContextProvider);
    }

    protected <T, ID extends Serializable> SimpleMybatisRepository<?, ?> getTargetRepository(
            SqlSessionTemplate sessionTemplate, RepositoryInformation information) {

        MybatisEntityInformationSupport<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());

        generateMapper(sessionTemplate.getConfiguration(), entityInformation);

        return getTargetRepositoryViaReflection(information, entityInformation, sessionTemplate);
    }

    private void generateMapper(Configuration configuration, MybatisEntityInformationSupport<?, ?> entityInformation) {

        MybatisSimpleRepositoryMapperGenerator generator = new MybatisSimpleRepositoryMapperGenerator(
                configuration, entityInformation.getModel(), entityInformation.getJavaType().getName(), "beetl");
        generator.generate();
    }


    private boolean isQueryDslExecutor(Class<?> repositoryInterface) {
        return QUERY_DSL_PRESENT && QueryDslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
    }


}
