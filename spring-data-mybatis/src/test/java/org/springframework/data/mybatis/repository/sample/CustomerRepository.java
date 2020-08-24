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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.domain.sample.Customer;
import org.springframework.data.mybatis.domain.sample.Customer.Constellation;
import org.springframework.data.mybatis.domain.sample.Customer.Gender;
import org.springframework.data.mybatis.domain.sample.Name;
import org.springframework.data.mybatis.repository.Modifying;
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

	List<Customer> findByNameFirstname(String firstname, Pageable pageable);

	Page<Customer> findByNameFirstnameIn(Pageable pageable, String... firstnames);

	List<Customer> findByNameFirstnameNotIn(Collection<String> firstnames);

	@Query("select * from #{#entityName} where firstname like ?1%")
	List<Customer> findByFirstnameLike(String firstname);

	@Query("select * from #{#entityName} where firstname like :firstname%")
	List<Customer> findByFirstnameLikeNamed(@Param("firstname") String firstname);

	@Modifying
	@Query("update #{#entityName} set lastname = ?1")
	void renameAllUsersTo(String lastname);

	List<Customer> findByNameLastname(String lastname);

	@Query("select count(*) from #{#entityName} where firstname = ?1")
	Long countWithFirstname(String firstname);

	@Query("select * from #{#entityName} u where u.lastname = :lastname or u.firstname = :firstname")
	List<Customer> findByLastnameOrFirstname(@Param("firstname") String foo, @Param("lastname") String bar);

	List<Customer> findByNameFirstnameOrNameLastname(@Param("lastname") String lastname,
			@Param("firstname") String firstname);

	List<Customer> findByNameLastnameLikeOrderByNameFirstnameDesc(String lastname);

	List<Customer> findByNameLastnameNotLike(String lastname);

	List<Customer> findByNameLastnameNotNull();

	List<Customer> findByEmailAddressNull();

	List<Customer> findByEmailAddressLike(String email, Sort sort);

	List<Customer> findBySpringDataNamedQuery(String lastname);

	List<Customer> findByNameLastnameIgnoringCase(String lastname);

	Page<Customer> findByNameLastnameIgnoringCase(Pageable pageable, String lastname);

	List<Customer> findByNameLastnameIgnoringCaseLike(String lastname);

	List<Customer> findByNameLastnameAndNameFirstnameAllIgnoringCase(String lastname, String firstname);

	List<Customer> findByAgeGreaterThanEqual(int age);

	List<Customer> findByAgeLessThanEqual(int age);

	@Query("select u.lastname from #{#entityName} u group by u.lastname")
	Page<String> findByLastnameGrouped(Pageable pageable);

	List<Customer> findByNameFirstnameStartingWith(String firstname);

	List<Customer> findByNameFirstnameEndingWith(String firstname);

	List<Customer> findByNameFirstnameContaining(String firstname);

	@Query("SELECT 1 FROM #{#entityName}")
	List<Integer> findOnesByNativeQuery();

	List<Customer> deleteByNameLastname(String lastname);

	List<Customer> findByBinaryData(byte[] data);

	Slice<Customer> findSliceByNameLastname(String lastname, Pageable pageable);

	Long removeByNameLastname(String lastname);

	Customer findTopByOrderByAgeAsc();

	Customer findTop1ByOrderByAgeAsc();

	List<Customer> findFirst2CustomersBy(Sort sort);

	List<Customer> findTop2CustomersBy(Sort sort);

	Page<Customer> findFirst3CustomersBy(Pageable page);

	Page<Customer> findFirst2CustomersBy(Pageable page);

	@Query("select * from #{#entityName} u where u.email_address = ?1")
	Optional<Customer> findOptionalByEmailAddress(String emailAddress);

	@Query("select * from #{#entityName} u where u.firstname = ?#{[0]} and u.firstname = ?1 and u.lastname like %?#{[1]}% and u.lastname like %?2%")
	List<Customer> findByFirstnameAndLastnameWithSpelExpression(String firstname, String lastname);

	@Query("select * from #{#entityName} u where u.firstname = ?#{'Oliver'}")
	List<Customer> findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

	@Query("select * from #{#entityName} u where u.age = ?#{[0]}")
	List<Customer> findUsersByAgeForSpELExpressionByIndexedParameter(int age);

	List<Customer> findByAgeIn(Collection<Integer> ages);

	List<Customer> queryByAgeIn(Integer[] ages);

	List<Customer> queryByAgeInOrNameFirstname(Integer[] ages, String firstname);

	@Query("select * from #{#tableName}")
	Stream<Customer> findAllByCustomQueryAndStream();

	Stream<Customer> readAllByNameFirstnameNotNull();

	@Query("select * from #{#tableName}")
	Stream<Customer> streamAllPaged(Pageable pageable);

	List<Customer> findByNameLastnameNotContaining(String part);

}
