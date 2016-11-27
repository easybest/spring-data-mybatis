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

package org.springframework.data.mybatis.repository.localism;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * @author Jarvis Song
 */
public class LocalismFactoryBean implements FactoryBean<Localism>, InitializingBean {

    private Localism          localism;
    private SqlSessionFactory sqlSessionFactory;

    @Override
    public Localism getObject() throws Exception {

        if (null == localism) {
            afterPropertiesSet();
        }
        return localism;
    }

    @Override
    public Class<?> getObjectType() {
        return null == localism ? Localism.class : localism.getClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(sqlSessionFactory);
        String databaseId = sqlSessionFactory.getConfiguration().getDatabaseId();
        localism = getLocalism(databaseId);
    }

    public static Localism getLocalism(String databaseId) {
        if ("h2".equalsIgnoreCase(databaseId)) {
            return new H2Localism();
        }

        if ("MySQL".equalsIgnoreCase(databaseId)) {
            return new MySQLLocalism();
        }

        if ("Oracle".equalsIgnoreCase(databaseId)) {
            return new OracleLocalism();
        }
        if ("sqlserver".equalsIgnoreCase(databaseId)) {
            return new SqlServerLocalism();
        }


        throw new IllegalArgumentException("not support database id: " + databaseId);
    }

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
}
