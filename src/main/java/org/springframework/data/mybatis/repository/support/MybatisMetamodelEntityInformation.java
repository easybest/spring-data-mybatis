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

import org.springframework.data.repository.core.support.AbstractEntityInformation;
import org.springframework.util.ReflectionUtils;

import javax.persistence.metamodel.SingularAttribute;
import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * mybatis meta.
 *
 * @author Jarvis Song
 */
public class MybatisMetamodelEntityInformation<T, ID extends Serializable> extends MybatisEntityInformationSupport<T, ID> {


    /**
     * Creates a new {@link AbstractEntityInformation} from the given domain class.
     *
     * @param domainClass must not be {@literal null}.
     */
    public MybatisMetamodelEntityInformation(Class<T> domainClass) {
        super(domainClass);
    }

    @Override
    public SingularAttribute<? super T, ?> getIdAttribute() {
        return null;
    }

    @Override
    public boolean hasCompositeId() {
        return model.getPrimaryKeys().size() > 1;
    }

    @Override
    public Iterable<String> getIdAttributeNames() {
        return null;
    }

    @Override
    public Object getCompositeIdAttributeValue(Serializable id, String idAttribute) {
        return null;
    }

    @Override
    public ID getId(T entity) {
        MybatisEntityModel primaryKey = model.getPrimaryKey();
        if (null == primaryKey) {
            return null;
        }

        Field field = ReflectionUtils.findField(entity.getClass(), primaryKey.getName());
        if (null == field) {
            return null;
        }

        return (ID) ReflectionUtils.getField(field, entity);

    }

    @Override
    public Class<ID> getIdType() {
        MybatisEntityModel primaryKey = model.getPrimaryKey();
        if (null == primaryKey) {
            return null;
        }
        return (Class<ID>) primaryKey.getClz();
    }
}
