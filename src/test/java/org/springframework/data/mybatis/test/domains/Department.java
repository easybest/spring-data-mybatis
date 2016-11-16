package org.springframework.data.mybatis.test.domains;

import org.springframework.data.mybatis.domains.LongId;

import javax.persistence.Entity;

/**
 * Created by songjiawei on 2016/11/13.
 */
@Entity
public class Department extends LongId {

    private String name;

    public Department() {
    }

    public Department(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
