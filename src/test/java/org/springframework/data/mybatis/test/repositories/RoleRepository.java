package org.springframework.data.mybatis.test.repositories;

import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.test.domains.Role;

/**
 * Created by songjiawei on 2016/11/10.
 */
public interface RoleRepository extends MybatisRepository<Role, Long> {

    Role getByName(String name);

}
