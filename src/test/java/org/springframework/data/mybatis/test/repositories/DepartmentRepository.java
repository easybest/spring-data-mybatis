package org.springframework.data.mybatis.test.repositories;

import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.test.domains.Department;

/**
 * Created by songjiawei on 2016/11/15.
 */
public interface DepartmentRepository extends MybatisRepository<Department, Long> {
}
