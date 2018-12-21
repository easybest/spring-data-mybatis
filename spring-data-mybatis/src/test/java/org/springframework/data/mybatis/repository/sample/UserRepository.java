/*
 * Copyright 2008-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mybatis.repository.sample;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.repository.DataSource;
import org.springframework.data.mybatis.repository.Modifying;
import org.springframework.data.mybatis.repository.MybatisRepository;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.data.mybatis.repository.SelectColumns;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for {@code User}s.
 */
@DataSource("ds2")
@Transactional(readOnly = true)
public interface UserRepository
		extends MybatisRepository<User, Integer>, UserRepositoryCustom {

	@Override
	User getById(Integer integer);

	List<User> findByLastname(String lastname);

	@Transactional
	java.util.Optional<User> findById(Integer primaryKey);

	@Transactional
	@Modifying
	@Query("delete from ds_user where id = ?1")
	void deleteById(Integer id);

	@Query(statement = "search_by_email")
	User findByEmailAddress(@Param("emailAddress") String emailAddress);

	User findByEmailAddressAndLastname(String emailAddress, String lastname);

	List<User> findByEmailAddressAndLastnameOrFirstname(String emailAddress,
			String lastname, String username);

	@Transactional
	@Modifying
	@Query("update ds_user set lastname=:lastname")
	void renameAllUsersTo(@Param("lastname") String lastname);

	List<User> findByFirstnameOrLastname(@Param("lastname") String lastname,
			@Param("firstname") String firstname);

	List<User> findByLastnameLikeOrderByFirstnameDesc(String lastname);

	List<User> findByLastnameNotLike(String lastname);

	List<User> findByLastnameNot(String lastname);

	List<User> findByLastnameNotNull();

	List<User> findByLastnameNull();

	List<User> findByEmailAddressLike(String email, Sort sort);

	List<User> findByLastnameIgnoringCase(String lastname);

	Page<User> findByLastnameIgnoringCase(Pageable pageable, String lastname);

	List<User> findByLastnameIgnoringCaseLike(String lastname);

	List<User> findByLastnameAndFirstnameAllIgnoringCase(String lastname,
			String firstname);

	List<User> findByAgeGreaterThanEqual(int age);

	List<User> findByAgeLessThanEqual(int age);

	List<User> findByActiveTrue();

	List<User> findByActiveFalse();

	List<User> findByCreatedAtBefore(Date date);

	List<User> findByCreatedAtAfter(Date date);

	List<User> findByFirstnameStartingWith(String firstname);

	List<User> findByFirstnameEndingWith(String firstname);

	List<User> findByFirstnameContaining(String firstname);

	Page<User> findByLastname(Pageable pageable, String lastname);

	List<User> findByFirstname(String firstname, Pageable pageable);

	Page<User> findByFirstnameIn(Pageable pageable, String... firstnames);

	List<User> findByFirstnameNotIn(Collection<String> firstnames);

	Collection<User> findByIdIn(@Param("ids") Integer... ids);

	long countByLastname(String lastname);

	int countUsersByFirstname(String firstname);

	boolean existsByLastname(String lastname);

	List<User> findAllByOrderByLastnameAsc();

	List<User> findByBinaryData(byte[] data);

	Slice<User> findSliceByLastname(String lastname, Pageable pageable);

	Long removeByLastname(String lastname);

	List<User> deleteByLastname(String lastname);

	User findFirstByOrderByAgeDesc();

	User findFirst1ByOrderByAgeDesc();

	User findTopByOrderByAgeDesc();

	User findTopByOrderByAgeAsc();

	User findTop1ByOrderByAgeAsc();

	List<User> findTop2ByOrderByAgeDesc();

	List<User> findFirst2ByOrderByAgeDesc();

	List<User> findFirst2UsersBy(Sort sort);

	List<User> findTop2UsersBy(Sort sort);

	Page<User> findFirst3UsersBy(Pageable page);

	Page<User> findFirst2UsersBy(Pageable page);

	Slice<User> findTop3UsersBy(Pageable page);

	Slice<User> findTop2UsersBy(Pageable page);

	List<User> findByAgeIn(Collection<Integer> ages);

	@SelectColumns("id,lastname")
	List<User> queryByAgeIn(Integer[] ages);

	List<User> queryByAgeInOrFirstname(Integer[] ages, String firstname);

	List<User> findByLastnameNotContaining(String part);

	<T> Stream<T> findAsStreamByFirstnameLike(String name, Class<T> projectionType);

	<T> List<T> findAsListByFirstnameLike(String name, Class<T> projectionType);

	@DataSource("ds3")
	@Query("select firstname from ds_user where lastname = ?1 limit 1")
	String getFirstnameByLastname(String lastname);

	@Query("select id from ds_user where firstname = ?1 limit 1")
	Integer getUserIdByFirstname(String firstname);

	@Query("select firstname from ds_user where lastname in ?1 order by firstname asc")
	List<String> findFirstnamesInLastnames(String... lastnames);

	@Query("select firstname from ds_user where lastname like %?1% order by firstname asc")
	List<String> findFirstnamesByLastnamesLike(String lastname);

}
