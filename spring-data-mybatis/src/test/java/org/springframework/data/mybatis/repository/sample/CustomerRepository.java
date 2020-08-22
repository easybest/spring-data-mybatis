/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.repository.sample;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mybatis.domain.sample.Customer;
import org.springframework.data.mybatis.domain.sample.Customer.Constellation;
import org.springframework.data.mybatis.domain.sample.Customer.Gender;
import org.springframework.data.mybatis.domain.sample.Name;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * .
 *
 * @author JARVIS SONG
 */
public interface CustomerRepository extends MybatisRepository<Customer, Name> {

	List<Customer> findByNameFirstnameLike(String firstname);

	@Query("select * from t_customer u where u.firstname like :firstname%")
	List<Customer> findByNameFirstnameLikeNamed(@Param("firstname") String firstname);

	long countByNameLastname(String lastname);

	int countCustomersByNameFirstname(String firstname);

	boolean existsByNameLastname(String lastname);

	@Query("select firstname from t_customer where lastname = ?1")
	List<String> findFirstnamesByLastname(String lastname);

	List<Customer> findAllByOrderByNameLastnameAsc();

	Customer findFirstByOrderByAgeDesc();

	Customer findFirst1ByOrderByAgeDesc();

	List<Customer> findFirst2ByOrderByAgeDesc();

	List<Customer> findTop2ByOrderByAgeDesc();

	Slice<Customer> findTop3CustomersBy(Pageable pageable);

	Slice<Customer> findTop2CustomersBy(Pageable pageable);

	List<Customer> findByGender(Gender gender);

	List<Customer> findByConstellation(Constellation constellation);

	Customer findByEmailAddress(String emailAddress);

	Customer findByEmailAddressAndNameLastname(String emailAddress, String lastname);

	List<Customer> findByEmailAddressAndNameLastnameOrNameFirstname(String emailAddress, String lastname,
			String username);

	@Query("select * from #{#entityName} where email_address = ?1")
	@Transactional(readOnly = true)
	Customer findByAnnotatedQuery(String emailAddress);

	Page<Customer> findByNameLastname(Pageable pageable, String lastname);

	Page<Customer> findByEmailAddress(Pageable pageable, String emailAddress);

}
