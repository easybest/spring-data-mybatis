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

import org.springframework.data.mybatis.repository.query.MybatisEntityMetadata;
import org.springframework.data.repository.core.EntityInformation;

import javax.persistence.metamodel.SingularAttribute;
import java.io.Serializable;

/**
 * mybatis entity information.
 *
 * @author Jarvis Song
 */
public interface MybatisEntityInformation<T, ID extends Serializable> extends EntityInformation<T, ID>, MybatisEntityMetadata<T> {

    /**
     * Returns the id attribute of the entity.
     *
     * @return SingularAttribute
     */
    SingularAttribute<? super T, ?> getIdAttribute();

    /**
     * Returns {@literal true} if the entity has a composite id.
     *
     * @return hasCompositeId
     */
    boolean hasCompositeId();

    /**
     * Returns the attribute names of the id attributes. If the entity has a composite id, then all id attribute names are
     * returned. If the entity has a single id attribute then this single attribute name is returned.
     *
     * @return IdAttributeNames
     */
    Iterable<String> getIdAttributeNames();

    /**
     * Extracts the value for the given id attribute from a composite id
     *
     * @param id
     * @param idAttribute
     * @return CompositeIdAttributeValue
     */
    Object getCompositeIdAttributeValue(Serializable id, String idAttribute);

}
