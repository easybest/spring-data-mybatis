package org.springframework.data.mybatis.repository.sample;

import java.util.List;

import org.springframework.data.mybatis.domain.sample.UserRole;
import org.springframework.data.mybatis.domain.sample.UserRoleKey;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserRoleRepository extends MybatisRepository<UserRole, UserRoleKey> {

	@Query("select * from " + UserRole.TABLE_NAME
			+ " where ds_role_id = ?1 order by ds_user_id asc")
	List<UserRole> findByRoleId(Long roleId);

}
