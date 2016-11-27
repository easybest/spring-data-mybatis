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

import org.springframework.data.annotations.ComplexSearch;
import org.springframework.data.annotations.Searchable;

import javax.persistence.*;

import static org.springframework.data.annotations.Searchable.OPERATE.LIKE;

/**
 * Sample domain class representing roles. Mapped with XML.
 */
@Entity
@Table(name = "DS_ROLE")
public class Role {

    private static final String PREFIX = "ROLE_";

    @Id
    @GeneratedValue
    private Integer id;
    @Searchable
    @ComplexSearch({
            @Searchable,
            @Searchable(operate = LIKE, properyName = "fuzzyName")
    })
    private String  name;

    @ManyToOne
    private Group group;

    /**
     * Creates a new instance of {@code Role}.
     */
    public Role() {
    }

    /**
     * Creates a new preconfigured {@code Role}.
     *
     * @param name
     */
    public Role(final String name) {
        this.name = name;
    }

    public Role(String name, Group group) {
        this.name = name;
        this.group = group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Role role = (Role) o;

        return id != null ? id.equals(role.id) : role.id == null;

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * Returns the id.
     *
     * @return
     */
    public Integer getId() {

        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return PREFIX + name;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }
}
