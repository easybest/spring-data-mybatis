package org.springframework.data.mybatis.test.domains;

import org.springframework.data.annotations.Searchable;
import org.springframework.data.mybatis.domains.LongId;

import javax.persistence.*;

/**
 * Created by songjiawei on 2016/11/9.
 */
@Entity
public class User extends LongId {

    @Searchable
    private String firstname;
    @Searchable(operate = Searchable.OPERATE.LIKE)
    private String lastname;

    @ManyToOne
    private Department department;

    private Integer age;


    public User() {
    }

    public User(String firstname, String lastname) {
        this.firstname = firstname;
        this.lastname = lastname;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }
}
