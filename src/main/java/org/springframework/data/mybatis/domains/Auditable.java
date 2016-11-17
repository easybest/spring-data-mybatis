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

package org.springframework.data.mybatis.domains;

import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.util.Date;

/**
 * Auditable Interface.
 *
 * @param <ID> Primary key's type.
 * @author Jarvis Song
 */
public interface Auditable<ID extends Serializable> extends Persistable<ID> {

    /**
     * Returns the user who created this entity.
     *
     * @return the createdBy
     */
    Long getCreatedBy();

    /**
     * Sets the user who created this entity.
     *
     * @param createdBy the creating entity to set
     */
    void setCreatedBy(final Long createdBy);

    /**
     * Returns the creation date of the entity.
     *
     * @return the createdDate
     */
    Date getCreatedDate();

    /**
     * Sets the creation date of the entity.
     *
     * @param creationDate the creation date to set
     */
    void setCreatedDate(final Date creationDate);

    /**
     * Returns the user who modified the entity lastly.
     *
     * @return the lastModifiedBy
     */
    Long getLastModifiedBy();

    /**
     * Sets the user who modified the entity lastly.
     *
     * @param lastModifiedBy the last modifying entity to set
     */
    void setLastModifiedBy(final Long lastModifiedBy);

    /**
     * Returns the date of the last modification.
     *
     * @return the lastModifiedDate
     */
    Date getLastModifiedDate();

    /**
     * Sets the date of the last modification.
     *
     * @param lastModifiedDate the date of the last modification to set
     */
    void setLastModifiedDate(final Date lastModifiedDate);
}
