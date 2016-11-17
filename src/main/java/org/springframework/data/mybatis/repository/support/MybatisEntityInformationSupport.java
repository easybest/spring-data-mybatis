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

import org.springframework.data.domain.Persistable;
import org.springframework.data.mybatis.repository.query.DefaultMybatisEntityMetadata;
import org.springframework.data.mybatis.repository.query.MybatisEntityMetadata;
import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * mybatis entity information support.
 *
 * @author Jarvis Song
 */
public abstract class MybatisEntityInformationSupport<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> implements MybatisEntityInformation<T, ID> {

    private   MybatisEntityMetadata<T> metadata;
    protected MybatisEntityModel       model;

    private static Map<Class<?>, MybatisEntityInformationSupport> cache = new HashMap<Class<?>, MybatisEntityInformationSupport>();

    /**
     * Creates a new {@link AbstractEntityInformation} from the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     */
    public MybatisEntityInformationSupport(Class<T> domainClass) {
        super(domainClass);
        this.metadata = new DefaultMybatisEntityMetadata<T>(domainClass);
        this.model = new MybatisEntityModel(domainClass);
    }

    public MybatisEntityModel getModel() {
        return model;
    }

    @Override
    public String getEntityName() {
        return metadata.getEntityName();
    }

    public static <T> MybatisEntityInformationSupport<T, ?> getEntityInformation(Class<T> domainClass) {
        Assert.notNull(domainClass);

        MybatisEntityInformationSupport information = cache.get(domainClass);
        if (null == information) {
            if (Persistable.class.isAssignableFrom(domainClass)) {
                information = new MybatisPersistableEntityInformation(domainClass);
            } else {
                information = new MybatisMetamodelEntityInformation(domainClass);
            }

            cache.put(domainClass, information);
        }
        return information;


    }
}
