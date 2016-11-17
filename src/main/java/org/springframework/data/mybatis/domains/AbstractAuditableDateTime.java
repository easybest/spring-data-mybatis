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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotations.JdbcType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * Auditable Basic Entity Only DateTime.
 *
 * @param <PK> primary key's type.
 * @author Jarvis Song
 */
@MappedSuperclass
public abstract class AbstractAuditableDateTime<PK extends Serializable> extends AbstractPersistable<PK> {

    private static final long serialVersionUID = 3732216348067110393L;


    @Column(name = "CREATED_DATE")
    @JdbcType(org.apache.ibatis.type.JdbcType.BIGINT)
    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    protected Date createdDate;


    @Column(name = "LAST_MODIFIED_DATE")
    @JdbcType(org.apache.ibatis.type.JdbcType.BIGINT)
    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    protected Date lastModifiedDate;

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
