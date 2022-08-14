/*
 * Copyright 2019-2022 the original author or authors.
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

package io.easybest.mybatis.repository.sample;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import io.easybest.mybatis.domain.sample.Role;
import io.easybest.mybatis.domain.sample.User;
import io.easybest.mybatis.repository.Modifying;
import io.easybest.mybatis.repository.MybatisRepository;
import io.easybest.mybatis.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
public interface UserRepository extends MybatisRepository<User, Integer>, UserRepositoryCustom {

	@Transactional
	@Modifying
	@Query("insert into SD_User_SD_User (User_id, colleagues_id) values (?1, ?2)")
	int saveColleague(Integer userId, Integer colleagueId);

	@Transactional
	@Modifying
	@Query("insert into SD_User_Role(User_id, roles_id) values (?1, ?2)")
	int saveRole(Integer userId, Integer roleId);

	@Transactional
	default void saveColleagues(Integer first, Integer second) {
		this.saveColleague(first, second);
		this.saveColleague(second, first);
	}

	@Transactional
	default User saveWithRoles(User user) {

		this.save(user);
		if (CollectionUtils.isEmpty(user.getRoles())) {
			return user;
		}

		user.getRoles().forEach(role -> this.saveRole(user.getId(), role.getId()));
		return user;
	}

	List<User> findByLastname(String lastname);

	User findByEmailAddress(String emailAddress);

	@Modifying
	@Query("update #{#tableName} set lastname = ?1")
	void renameAllUsersTo(String lastname);

	@Query("select u from #{#tableName} u where u.emailAddress = ?1")
	@Transactional(readOnly = true)
	User findByAnnotatedQuery(String emailAddress);

	@Query("select count(u) from #{#tableName} u where u.firstname = ?1")
	Long countWithFirstname(String firstname);

	@Query("select u from #{#tableName} u where u.lastname = :lastname or u.firstname = :firstname")
	List<User> findByLastnameOrFirstname(@Param("firstname") String foo, @Param("lastname") String bar);

	List<User> findByFirstnameOrLastname(@Param("lastname") String lastname, @Param("firstname") String firstname);

	List<User> findByLastnameLikeOrderByFirstnameDesc(String lastname);

	List<User> findByManagerLastname(String name);

	List<User> findByLastnameNotLike(String lastname);

	List<User> findByLastnameNot(String lastname);

	List<User> findByFirstnameStartingWith(String firstname);

	List<User> findByColleaguesLastname(String lastname);

	List<User> findByLastnameNotNull();

	List<User> findByLastnameNull();

	List<User> findByEmailAddressLike(String email, Sort sort);

	List<User> findBySpringDataNamedQuery(String lastname);

	@Query("select u.lastname from #{#tableName} u group by u.lastname")
	Page<String> findByLastnameGrouped(Pageable pageable);

	List<User> findByAgeGreaterThanEqual(int age);

	List<User> findByAgeLessThanEqual(int age);

	List<User> findByActiveTrue();

	List<User> findByActiveFalse();

	// @Query("select u.colleagues from #{#tableName} u where u = ?1")
	// List<User> findColleaguesFor(User user);

	List<User> findByCreatedAtAfter(Date date);

	List<User> findByCreatedAtBefore(Date date);

	List<User> findByFirstnameEndingWith(String firstname);

	List<User> findByFirstnameContaining(String firstname);

	List<User> findByFirstname(String firstname, Pageable pageable);

	Page<User> findByFirstnameIn(Pageable pageable, String... firstnames);

	List<User> findByFirstnameNotIn(Collection<String> firstnames);

	@Query("select u from #{#tableName} u left outer join #{#tableName} as manager on u.manager_id=manager.id")
	Page<User> findAllPaged(Pageable pageable);

	@Query("select u from #{#tableName} u where u.firstname like ?1%")
	List<User> findByFirstnameLike(String firstname);

	@Query("select u from #{#tableName} u where u.firstname like :firstname%")
	List<User> findByFirstnameLikeNamed(@Param("firstname") String firstname);

	long countByLastname(String lastname);

	int countUsersByFirstname(String firstname);

	boolean existsByLastname(String lastname);

	@Query("select u.firstname from #{#tableName} u where u.lastname = ?1")
	List<String> findFirstnamesByLastname(String lastname);

	Collection<User> findByIdIn(@Param("ids") Integer... ids);

	@Modifying
	@Query("update #{#tableName} set active = :activeState where id in :ids")
	void updateUserActiveState(@Param("activeState") boolean activeState, @Param("ids") Integer... ids);

	List<User> findAllByOrderByLastnameAsc();

	List<User> findByBinaryData(byte[] data);

	@Query("select u from #{#tableName} u where u.id in ?1")
	Collection<User> findByIdsCustomWithPositionalVarArgs(Integer... ids);

	@Query("select u from #{#tableName} u where u.id in :ids")
	Collection<User> findByIdsCustomWithNamedVarArgs(@Param("ids") Integer... ids);

	List<User> deleteByLastname(String lastname);

	Long removeByLastname(String lastname);

	@Query("select u.binaryData from SD_User u where u.id = ?1")
	byte[] findBinaryDataByIdNative(Integer id);

	@Query(value = "select u from #{#tableName} u where u.firstname like ?1%", countProjection = "u.firstname")
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

	@Query("select u from #{#tableName} u where u.emailAddress = ?1")
	Optional<User> findOptionalByEmailAddress(String emailAddress);

	@Query("select u from #{#tableName} u where u.firstname = ?#{[0]} and u.firstname = ?1 and u.lastname like %?#{[1]}% and u.lastname like %?2%")
	List<User> findByFirstnameAndLastnameWithSpelExpression(String firstname, String lastname);

	@Query("select u from #{#tableName} u where u.lastname like %:#{[0]}% and u.lastname like %:lastname%")
	List<User> findByLastnameWithSpelExpression(@Param("lastname") String lastname);

	@Query("select u from #{#tableName} u where u.firstname = ?#{'Oliver'}")
	List<User> findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

	@Query("select u from #{#tableName} u where u.firstname = :#{'Oliver'}")
	List<User> findOliverBySpELExpressionWithoutArgumentsWithColon();

	@Query("select u from #{#tableName} u where u.age = ?#{[0]}")
	List<User> findUsersByAgeForSpELExpressionByIndexedParameter(int age);

	@Query("select u from #{#tableName} u where u.firstname = :firstname and u.firstname = :#{#firstname}")
	List<User> findUsersByFirstnameForSpELExpression(@Param("firstname") String firstname);

	@Query("select u from #{#tableName} u where u.firstname = :#{#firstname}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterVariableOnly(@Param("firstname") String firstname);

	@Query("select u from #{#tableName} u where u.firstname = ?#{[0]}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterIndexOnly(String firstname);

	@Query(value = "select * from (" //
			+ "select u.*, rownum() as RN from (" //
			+ "select * from SD_User ORDER BY ucase(firstname)" //
			+ ") u" //
			+ ") where RN between ?#{ #pageable.offset +1 } and ?#{#pageable.offset + #pageable.pageSize}", //
			countQuery = "select count(u.id) from SD_User u")
	Page<User> findUsersInNativeQueryWithPagination(Pageable pageable);

	@Query("select u from #{#tableName} u where u.firstname =:#{#user.firstname} and u.lastname =:lastname")
	List<User> findUsersByUserFirstnameAsSpELExpressionAndLastnameAsString(@Param("user") User user,
			@Param("lastname") String lastname);

	@Query("select u from #{#tableName} u where u.firstname =:firstname and u.lastname =:#{#user.lastname}")
	List<User> findUsersByFirstnameAsStringAndUserLastnameAsSpELExpression(@Param("firstname") String firstname,
			@Param("user") User user);

	@Query("select u from #{#tableName} u where u.firstname =:#{#user.firstname} and u.lastname =:#{#lastname}")
	List<User> findUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpression(@Param("user") User user,
			@Param("lastname") String lastname);

	@Query("select u from #{#tableName} u where u.firstname =:#{#firstname} and u.lastname =:#{#user.lastname}")
	List<User> findUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpression(
			@Param("firstname") String firstname, @Param("user") User user);

	@Query("select u from #{#tableName} u where u.firstname =:firstname")
	List<User> findUsersByFirstnamePaginated(Pageable page, @Param("firstname") String firstname);

	@Query("select u from #{#tableName} u where u.firstname = ?#{[0]} and u.lastname = ?#{[1]}")
	List<User> findUsersByFirstnameForSpELExpressionWithParameterIndexOnlyWithEntityExpression(String firstname,
			String lastname);

	List<User> findByAgeIn(Collection<Integer> ages);

	Page<User> findByAgeIn(Collection<Integer> ages, Pageable pageable);

	Page<User> findByAgeIn(Collection<Integer> ages, PageRequest pageable);

	List<User> queryByAgeIn(Integer[] ages);

	List<User> queryByAgeInOrFirstname(Integer[] ages, String firstname);

	@Query("select u from #{#tableName} u")
	Stream<User> findAllByCustomQueryAndStream();

	Stream<User> readAllByFirstnameNotNull();

	@Query("select u from #{#tableName} u")
	Stream<User> streamAllPaged(Pageable pageable);

	<T> Stream<T> findAsStreamByFirstnameLike(String name, Class<T> projectionType);

	<T> List<T> findAsListByFirstnameLike(String name, Class<T> projectionType);

	@Query("select u from #{#tableName} u where u.firstname = :#{#firstname} and u.firstname = :#{#firstname}")
	List<User> findUsersByDuplicateSpel(@Param("firstname") String firstname);

	@Query("SELECT firstname, lastname FROM SD_User WHERE id = ?1")
	NameOnly findByNativeQuery(Integer id);

	@Query("SELECT emailaddress FROM SD_User WHERE id = ?1")
	EmailOnly findEmailOnlyByNativeQuery(Integer id);

	@Query("SELECT u FROM #{#tableName} u where u.firstname >= ?1 and u.lastname = '000:1'")
	List<User> queryWithIndexedParameterAndColonFollowedByIntegerInString(String firstname);

	@Query("SELECT u FROM #{#tableName} u ORDER BY CASE WHEN (u.firstname  >= :name) THEN 0 ELSE 1 END, u.firstname")
	Page<User> findAllOrderedBySpecialNameSingleParam(@Param("name") String name, Pageable page);

	@Query("SELECT u FROM #{#tableName} u WHERE :other = 'x' ORDER BY CASE WHEN (u.firstname  >= :name) THEN 0 ELSE 1 END, u.firstname")
	Page<User> findAllOrderedBySpecialNameMultipleParams(@Param("name") String name, @Param("other") String other,
			Pageable page);

	@Query("SELECT u FROM #{#tableName} u WHERE ?2 = 'x' ORDER BY CASE WHEN (u.firstname  >= ?1) THEN 0 ELSE 1 END, u.firstname")
	Page<User> findAllOrderedBySpecialNameMultipleParamsIndexed(String name, String other, Pageable page);

	@Query(value = "SELECT firstname FROM SD_User ORDER BY UCASE(firstname)",
			countQuery = "SELECT count(*) FROM SD_User")
	Page<String> findByNativeQueryWithPageable(@Param("pageable") Pageable pageable);

	List<NameOnly> findByNamedQueryWithAliasInInvertedOrder();

	@Query("select firstname as \"firstname\", lastname as \"lastname\" from #{#tableName} u where u.firstname = 'Oliver'")
	Map<String, Object> findMapWithNullValues();

	@Query("select * from SD_User u where u.emailAddress = ?1")
	User findByEmailNativeAddressJdbcStyleParameter(String emailAddress);

	List<NameOnlyDto> findAllDtoProjectedBy();

	List<NameOnly> findAllInterfaceProjectedBy();

	@Modifying
	@Query("update SD_User set active = false where id = :userId")
	void setActiveToFalseWithModifyingNative(@Param("userId") int userId);

	@Query("select concat(?1,u.id,?2) as idWithPrefixAndSuffix from #{#tableName} u")
	List<String> findAllAndSortByFunctionResultPositionalParameter(
			@Param("positionalParameter1") String positionalParameter1,
			@Param("positionalParameter2") String positionalParameter2, Sort sort);

	@Query("select concat(:namedParameter1,u.id,:namedParameter2) as idWithPrefixAndSuffix from #{#tableName} u")
	List<String> findAllAndSortByFunctionResultNamedParameter(@Param("namedParameter1") String namedParameter1,
			@Param("namedParameter2") String namedParameter2, Sort sort);

	User findByEmailAddressAndLastname(String emailAddress, String lastname);

	List<User> findByEmailAddressAndLastnameOrFirstname(String emailAddress, String lastname, String username);

	Page<User> findByLastname(Pageable pageable, String lastname);

	List<User> findByLastnameIgnoringCase(String lastname);

	Page<User> findByLastnameIgnoringCase(Pageable pageable, String lastname);

	List<User> findByLastnameIgnoringCaseLike(String lastname);

	List<User> findByLastnameAndFirstnameAllIgnoringCase(String lastname, String firstname);

	Slice<User> findSliceByLastname(String lastname, Pageable pageable);

	List<User> findByLastnameNotContaining(String part);

	@Query("select u from #{#tableName} u where u.lastname like %?#{escape([0])}% escape ?#{escapeCharacter()}")
	List<User> findContainingEscaped(String namePart);

	List<User> findByRolesContaining(Role role);

	List<User> findByRolesNotContaining(Role role);

	List<RolesAndFirstname> findRolesAndFirstnameBy();

	List<NameOnlyDto> findByNamedQueryWithConstructorExpression();

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
