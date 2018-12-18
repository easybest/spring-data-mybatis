package org.springframework.data.mybatis.repository.sample;

import org.springframework.data.mybatis.domain.sample.UserRole;
import org.springframework.data.mybatis.domain.sample.UserRoleKey;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserRoleRepository extends MybatisRepository<UserRole, UserRoleKey> {

}
