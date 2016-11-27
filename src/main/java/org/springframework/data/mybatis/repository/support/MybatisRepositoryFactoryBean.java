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

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.mybatis.repository.localism.Localism;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * spring data repository factory bean.
 *
 * @author Jarvis Song
 */
public class MybatisRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends
        TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

    private SqlSessionTemplate sqlSessionTemplate;
    private Localism           localism;

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(sqlSessionTemplate, "SqlSessionTemplate must not be null.");
        Assert.notNull(localism, "database localism must not be null.");
        super.afterPropertiesSet();


    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return createRepositoryFactory(sqlSessionTemplate, localism);
    }

    private RepositoryFactorySupport createRepositoryFactory(SqlSessionTemplate sessionTemplate, Localism localism) {
        return new MybatisRepositoryFactory(sessionTemplate, localism);
    }

    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    public void setLocalism(Localism localism) {
        this.localism = localism;
    }


}
