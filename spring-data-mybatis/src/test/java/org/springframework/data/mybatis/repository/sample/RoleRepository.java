package org.springframework.data.mybatis.repository.sample;

import org.springframework.data.mybatis.domain.sample.Role;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface RoleRepository extends MybatisRepository<Role, Long> {

}
