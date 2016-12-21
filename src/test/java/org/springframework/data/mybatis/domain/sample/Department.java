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

package org.springframework.data.mybatis.domain.sample;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mybatis.annotations.*;

import java.util.Date;

import static org.apache.ibatis.type.JdbcType.BIGINT;
import static org.springframework.data.mybatis.annotations.Id.GenerationType.AUTO;
import static org.springframework.data.mybatis.annotations.Temporal.TemporalType.TIMESTAMP;


/**
 * @author Jarvis Song
 */
@Entity
public class Department {
    @Id(strategy = AUTO)
    protected Long   id;
    private   String name;
    @Version
    private   Long   version;


    @Column(name = "CREATED_DATE")
    @JdbcType(BIGINT)
    @CreatedDate
    protected Date createdDate;


    @Column(name = "LAST_MODIFIED_DATE")
    @JdbcType(BIGINT)
    @Temporal(TIMESTAMP)
    @LastModifiedDate
    protected Date lastModifiedDate;

    @Column(name = "CREATOR")
    protected Long createdBy;

    @Column(name = "MODIFIER")
    protected Long lastModifiedBy;

    public Department() {
    }

    public Department(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(Long lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public void setName(String name) {

        this.name = name;
    }
}
