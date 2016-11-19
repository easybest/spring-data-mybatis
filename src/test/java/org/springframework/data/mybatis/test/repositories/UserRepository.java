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

    Long countByLastname(String lastname);

}
