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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.mybatis.domain.sample.Role;
import org.springframework.data.mybatis.domain.sample.SpecialUser;
import org.springframework.data.mybatis.domain.sample.User;
import org.springframework.data.mybatis.domain.sample.UserExample;
import org.springframework.data.mybatis.repository.Modifying;
import org.springframework.data.mybatis.repository.MybatisExampleRepository;
import org.springframework.data.mybatis.repository.Procedure;
import org.springframework.data.mybatis.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for {@code User}s.
 *
 * @author JARVIS SONG
 */
public interface UserRepository extends MybatisExampleRepository<User, Integer, UserExample>, UserRepositoryCustom {

	List<User> findByLastname(String lastname);

	@Transactional
	Optional<User> findById(Integer primaryKey);

	void deleteById(Integer id); // DATACMNS-649

	User findByEmailAddress(String emailAddress);

	@Query("select * from User u left outer join u.manager as manager")
	Page<User> findAllPaged(Pageable pageable);

	User findByEmailAddressAndLastname(String emailAddress, String lastname);

	List<User> findByEmailAddressAndLastnameOrFirstname(String emailAddress, String lastname, String username);

	@Query("select * from User u where u.emailAddress = ?1")
	@Transactional(readOnly = true)
	User findByAnnotatedQuery(String emailAddress);

	Page<User> findByLastname(Pageable pageable, String lastname);

	List<User> findByFirstname(String firstname, Pageable pageable);

	Page<User> findByFirstnameIn(Pageable pageable, String... firstnames);

	List<User> findByFirstnameNotIn(Collection<String> firstnames);

	@Query("select * from user u where u.firstname like ?1%")
	List<User> findByFirstnameLike(String firstname);

	@Query("select * from User u where u.firstname like :firstname%")
	List<User> findByFirstnameLikeNamed(@Param("firstname") String firstname);

	@Modifying
	@Query("update user set lastname = ?1")
	void renameAllUsersTo(String lastname);

	@Query("select count(*) from user u where u.firstname = ?1")
	Long countWithFirstname(String firstname);

	@Query("select * from user u where u.lastname = :lastname or u.firstname = :firstname")
	List<User> findByLastnameOrFirstname(@Param("firstname") String foo, @Param("lastname") String bar);

	@Query("select * from user u where u.lastname = :lastname or u.firstname = :firstname")
	List<User> findByLastnameOrFirstnameUnannotated(String firstname, String lastname);

	List<User> findByFirstnameOrLastname(@Param("lastname") String lastname, @Param("firstname") String firstname);

	List<User> findByLastnameLikeOrderByFirstnameDesc(String lastname);

	List<User> findByLastnameNotLike(String lastname);

	List<User> findByLastnameNot(String lastname);

	List<User> findByManagerLastname(String name);

	List<User> findByColleaguesLastname(String lastname);

	List<User> findByLastnameNotNull();

	List<User> findByLastnameNull();

	List<User> findByEmailAddressLike(String email, Sort sort);

	List<SpecialUser> findSpecialUsersByLastname(String lastname);

	List<User> findBySpringDataNamedQuery(String lastname);

	List<User> findByLastnameIgnoringCase(String lastname);

	Page<User> findByLastnameIgnoringCase(Pageable pageable, String lastname);

	List<User> findByLastnameIgnoringCaseLike(String lastname);

	List<User> findByLastnameAndFirstnameAllIgnoringCase(String lastname, String firstname);

	List<User> findByAgeGreaterThanEqual(int age);

	List<User> findByAgeLessThanEqual(int age);

	@Query("select u.lastname from user u group by u.lastname")
	Page<String> findByLastnameGrouped(Pageable pageable);

	@Query("SELECT * FROM user WHERE lastname = ?1")
	List<User> findNativeByLastname(String lastname);

	List<User> findByActiveTrue();

	List<User> findByActiveFalse();

	@Query("select u.colleagues from User u where u = ?1")
	List<User> findColleaguesFor(User user);

	List<User> findByCreatedAtBefore(Date date);

	List<User> findByCreatedAtAfter(Date date);

	List<User> findByFirstnameStartingWith(String firstname);

	List<User> findByFirstnameEndingWith(String firstname);

	List<User> findByFirstnameContaining(String firstname);

	@Query("SELECT 1 FROM user")
	List<Integer> findOnesByNativeQuery();

	long countByLastname(String lastname);

	int countUsersByFirstname(String firstname);

	boolean existsByLastname(String lastname);

	@Query("select u.firstname from user u where u.lastname = ?1")
	List<String> findFirstnamesByLastname(String lastname);

	Collection<User> findByIdIn(@Param("ids") Integer... ids);

	@Query("select * from user u where u.id in ?1")
	Collection<User> findByIdsCustomWithPositionalVarArgs(Integer... ids);

	@Query("select * from User u where u.id in :ids")
	Collection<User> findByIdsCustomWithNamedVarArgs(@Param("ids") Integer... ids);

	@Modifying
	@Query("update #{#entityName} u set u.active = :activeState where u.id in :ids")
	void updateUserActiveState(@Param("activeState") boolean activeState, @Param("ids") Integer... ids);

	List<User> findAllByOrderByLastnameAsc();

	List<User> findByBinaryData(byte[] data);

	Slice<User> findSliceByLastname(String lastname, Pageable pageable);

	List<User> findByAttributesIn(Set<String> attributes);

	Long removeByLastname(String lastname);

	List<User> deleteByLastname(String lastname);

	// @Query(value = "select u.binaryData from User u where u.id = :id")
	// byte[] findBinaryDataByIdJpaQl(@Param("id") Integer id);

	@Procedure("plus1inout")
	Integer explicitlyNamedPlus1inout(Integer arg);

	@Procedure(procedureName = "plus1inout")
	Integer plus1inout(Integer arg);

	@Procedure(name = "User.plus1IO")
	Integer entityAnnotatedCustomNamedProcedurePlus1IO(@Param("arg") Integer arg);

	@Procedure
	Integer plus1(@Param("arg") Integer arg);

	@Query(value = "select * from user u where u.firstname like ?1%", countProjection = "u.firstname")
	Page<User> findAllByFirstnameLike(String firstname, Pageable page);

	@Query(name = "User.findBySpringDataNamedQuery", countProjection = "u.firstname")
	Page<User> findByNamedQueryAndCountProjection(String firstname, Pageable page);

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

	@Query("select u.binaryData from user u where u.id = ?1")
	byte[] findBinaryDataByIdNative(Integer id);

	@Query("select * from User u where u.emailAddress = ?1")
	Optional<User> findOptionalByEmailAddress(String emailAddress);

	@Query("select * from User u where u.firstname = ?#{[0]} and u.firstname = ?1 and u.lastname like %?#{[1]}% and u.lastname like %?2%")
	List<User> findByFirstnameAndLastnameWithSpelExpression(String firstname, String lastname);

	@Query("select * from User u where u.lastname like %:#{[0]}% and u.lastname like %:lastname%")
	List<User> findByLastnameWithSpelExpression(@Param("lastname") String lastname);

	@Query("select * from User u where u.firstname = ?#{'Oliver'}")
	List<User> findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

	@Query("select * from User u where u.firstname = :#{'Oliver'}")
	List<User> findOliverBySpELExpressionWithoutArgumentsWithColon();

	@Query("select * from User u where u.age = ?#{[0]}")
	List<User> findUsersByAgeForSpELExpressionByIndexedParameter(int age);

	@Query("select * from User u where u.firstname = :firstname and u.firstname = :#{#firstname}")
	List<User> findUsersByFirstnameForSpELExpression(@Param("firstname") String firstname);

	@Query("select * from User u where u.emailAddress = ?#{principal.emailAddress}")
	List<User> findCurrentUserWithCustomQuery();

	@Query("select * from User u where u.firstname = ?1 and u.firstname=?#{[0]} and u.emailAddress = ?#{principal.emailAddress}")
	List<User> findByFirstnameAndCurrentUserWithCustomQuery(String firstname);

	@Query("select * from User u where u.firstname = :#{#firstname}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterVariableOnly(@Param("firstname") String firstname);

	@Query("select * from User u where u.firstname = ?#{[0]}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterIndexOnly(String firstname);

	@Query(value = "select * from (" + "select u.*, rownum() as RN from ("
			+ "select * from user ORDER BY ucase(firstname)" + ") u"
			+ ") where RN between ?#{ #pageable.offset +1 } and ?#{#pageable.offset + #pageable.pageSize}",
			countQuery = "select count(u.id) from user u")
	Page<User> findUsersInNativeQueryWithPagination(Pageable pageable);

	@Query("select * from User u where u.firstname =:#{#user.firstname} and u.lastname =:lastname")
	List<User> findUsersByUserFirstnameAsSpELExpressionAndLastnameAsString(@Param("user") User user,
			@Param("lastname") String lastname);

	@Query("select * from User u where u.firstname =:firstname and u.lastname =:#{#user.lastname}")
	List<User> findUsersByFirstnameAsStringAndUserLastnameAsSpELExpression(@Param("firstname") String firstname,
			@Param("user") User user);

	@Query("select * from User u where u.firstname =:#{#user.firstname} and u.lastname =:#{#lastname}")
	List<User> findUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpression(@Param("user") User user,
			@Param("lastname") String lastname);

	@Query("select * from User u where u.firstname =:#{#firstname} and u.lastname =:#{#user.lastname}")
	List<User> findUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpression(
			@Param("firstname") String firstname, @Param("user") User user);

	@Query("select * from User u where u.firstname =:firstname")
	List<User> findUsersByFirstnamePaginated(Pageable page, @Param("firstname") String firstname);

	@Query("select * from #{#entityName} u where u.firstname = ?#{[0]} and u.lastname = ?#{[1]}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterIndexOnlyWithEntityExpression(String firstname,
			String lastname);

	List<User> findByAgeIn(Collection<Integer> ages);

	List<User> queryByAgeIn(Integer[] ages);

	List<User> queryByAgeInOrFirstname(Integer[] ages, String firstname);

	@Query("select * from User u")
	Stream<User> findAllByCustomQueryAndStream();

	Stream<User> readAllByFirstnameNotNull();

	@Query("select * from User u")
	Stream<User> streamAllPaged(Pageable pageable);

	List<User> findByLastnameNotContaining(String part);

	List<User> findByRolesContaining(Role role);

	List<User> findByRolesNotContaining(Role role);

	List<User> findByRolesNameContaining(String name);

	@Query("select * from User u where u.firstname = :#{#firstname} and u.firstname = :#{#firstname}")
	List<User> findUsersByDuplicateSpel(@Param("firstname") String firstname);

	List<RolesAndFirstname> findRolesAndFirstnameBy();

	@Query("select * from User u where u.age = :age")
	List<User> findByStringAge(@Param("age") String age);

	<T> Stream<T> findAsStreamByFirstnameLike(String name, Class<T> projectionType);

	<T> List<T> findAsListByFirstnameLike(String name, Class<T> projectionType);

	@Query("SELECT firstname, lastname FROM user WHERE id = ?1")
	NameOnly findByNativeQuery(Integer id);

	@Query("SELECT emailaddress FROM user WHERE id = ?1")
	EmailOnly findEmailOnlyByNativeQuery(Integer id);

	@Query("select * FROM User u where u.firstname >= ?1 and u.lastname = '000:1'")
	List<User> queryWithIndexedParameterAndColonFollowedByIntegerInString(String firstname);

	@Query("select * FROM User u ORDER BY CASE WHEN (u.firstname  >= :name) THEN 0 ELSE 1 END, u.firstname")
	Page<User> findAllOrderedBySpecialNameSingleParam(@Param("name") String name, Pageable page);

	@Query("select * FROM User u WHERE :other = 'x' ORDER BY CASE WHEN (u.firstname  >= :name) THEN 0 ELSE 1 END, u.firstname")
	Page<User> findAllOrderedBySpecialNameMultipleParams(@Param("name") String name, @Param("other") String other,
			Pageable page);

	@Query("select * FROM User u WHERE ?2 = 'x' ORDER BY CASE WHEN (u.firstname  >= ?1) THEN 0 ELSE 1 END, u.firstname")
	Page<User> findAllOrderedBySpecialNameMultipleParamsIndexed(String name, String other, Pageable page);

	Page<User> findByNativeNamedQueryWithPageable(Pageable pageable);

	@Query(value = "SELECT firstname FROM user ORDER BY UCASE(firstname)", countQuery = "SELECT count(*) FROM user")
	Page<String> findByNativeQueryWithPageable(@Param("pageable") Pageable pageable);

	List<NameOnly> findByNamedQueryWithAliasInInvertedOrder();

	@Query("select firstname as firstname, lastname as lastname from User u where u.firstname = 'Oliver'")
	Map<String, Object> findMapWithNullValues();

	@Query("select * from user u where u.emailAddress = ?1")
	User findByEmailNativeAddressJdbcStyleParameter(String emailAddress);

	List<NameOnlyDto> findByNamedQueryWithConstructorExpression();

	@Query("select * from User u where u.lastname like %?#{escape([0])}% escape ?#{escapeCharacter()}")
	List<User> findContainingEscaped(String namePart);

	interface RolesAndFirstname {

		String getFirstname();

		Set<Role> getRoles();

	}

	interface NameOnly {

		String getFirstname();

		String getLastname();

	}

	interface EmailOnly {

		String getEmailAddress();

	}

}
