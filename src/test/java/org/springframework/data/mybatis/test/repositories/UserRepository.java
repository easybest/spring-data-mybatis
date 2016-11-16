package org.springframework.data.mybatis.test.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.test.domains.User;

import java.util.List;

/**
 * Created by songjiawei on 2016/11/9.
 */
public interface UserRepository extends MybatisRepository<User, Long> {

    List<User> findByLastnameOrderByFirstnameAsc(String lastname);

    Page<User> findByLastnameOrderByLastnameAsc(String lastname, Pageable pageable);

    User getByFirstnameAndLastname(String firstname, String lastname);


    List<User> findByLastname(String lastname, Sort sort);

}
