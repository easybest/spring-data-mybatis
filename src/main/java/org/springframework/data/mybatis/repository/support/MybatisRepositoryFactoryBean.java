package org.springframework.data.mybatis.repository.support;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * Created by songjiawei on 2016/11/9.
 */
public class MybatisRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends
        TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

    private final SqlSessionTemplate sessionTemplate;

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(sessionTemplate, "SqlSessionTemplate must not be null.");
        super.afterPropertiesSet();


        createMybatisMappers();
    }

    private void createMybatisMappers() {


    }

    @Autowired
    public MybatisRepositoryFactoryBean(SqlSessionTemplate sessionTemplate) {
        this.sessionTemplate = sessionTemplate;
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return createRepositoryFactory(sessionTemplate);
    }

    private RepositoryFactorySupport createRepositoryFactory(SqlSessionTemplate sessionTemplate) {
        return new MybatisRepositoryFactory(sessionTemplate);
    }


}
